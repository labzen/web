package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.annotation.CallService
import cn.labzen.web.annotation.LabzenWeb
import cn.labzen.web.annotation.MappingServiceVersion
import cn.labzen.web.source.ControllerMappingServiceVersionHelper
import cn.labzen.web.source.ControllerMeta
import cn.labzen.web.source.ControllerSourceGeneratorHelper
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.bytecode.ConstPool
import javassist.bytecode.MethodParametersAttribute
import javassist.bytecode.ParameterAnnotationsAttribute
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import kotlin.reflect.full.findAnnotations

internal class DeclaredInterfaceMethodGenerator(
  private val controllerMeta: ControllerMeta,
  private val interfaceMethod: Method
) {

  private lateinit var callServiceClass: Class<*>
  private lateinit var callMethodName: String
  private lateinit var callServiceMethod: Method
//  private var effectiveServiceMethod = false

  fun generate() {
    val methodParameterNames = interfaceMethod.parameters.joinToString(",") {
      "${it.type.name} ${it.name}"
    }
    val signature = "public ${interfaceMethod.returnType.name} ${interfaceMethod.name}($methodParameterNames)"

    // 生成方法体
    val body = generateDeclaredInterfaceMethodBody()
    // 创建方法
    val ctMethod = CtNewMethod.make(
      """
      $signature {
        $body
      }
    """.trimIndent(), controllerMeta.clazz
    )

    // 设置方法参数名与注解
    setupDeclaredInterfaceMethodArguments(ctMethod)
    // 转换方法的注解
    copyDeclaredInterfaceMethodAnnotations(ctMethod)

    controllerMeta.clazz.addMethod(ctMethod)
  }

  private fun findServiceTarget() {
    if (interfaceMethod.isAnnotationPresent(CallService::class.java)) {
      val annotation = interfaceMethod.getAnnotation(CallService::class.java)

      if (annotation.method.isNotBlank()) {
        callMethodName = annotation.method
      } else interfaceMethod.name

      callServiceClass = if (annotation.handler.java == Any::class.java) {
        controllerMeta.services[controllerMeta.mainServiceFieldName]!!
      } else
        annotation.handler.java
    } else {
      callMethodName = interfaceMethod.name
      callServiceClass = controllerMeta.services[controllerMeta.mainServiceFieldName]!!
    }
  }

  private fun generateDeclaredInterfaceMethodBody(): String {
    findServiceTarget()

    with(controllerMeta) {
      val controllerReturnType = interfaceMethod.returnType

      if (controllerReturnType == Void::class.java) {
        logger.warn().status(Status.REMIND)
          .log("Controller定义 [$interfaceType]，方法 [${interfaceMethod.name}] 未提供返回值")
        return ""
      }
      if (!services.containsValue(callServiceClass)) {
        logger.warn().status(Status.FIXME)
          .log("Controller定义 [$interfaceType]，未在ServiceHandler注解中指定 Service [${callServiceClass}]")
        return "return null;"
      }

      callServiceMethod = try {
        callServiceClass.getDeclaredMethod(callMethodName, *interfaceMethod.parameterTypes)
      } catch (e: NoSuchMethodException) {
        logger.warn().status(Status.FIXME)
          .log("Controller定义 [$interfaceType]，在指定的 Service [${callServiceClass}] 中找不到可调用的方法 - [$callMethodName(${interfaceMethod.parameterTypes})]")
        return "return null;"
      }

      val serviceFieldName = services.inverse()[callServiceClass]
      val serviceReturnType = callServiceMethod.returnType

      return if (serviceReturnType == Void::class.java) {
        logger.warn().status(Status.REMIND)
          .log("Controller定义 [$interfaceType]，Service方法 [$callMethodName(${interfaceMethod.parameterTypes})] 未提供返回值")
        "return null;"
      } else if (controllerReturnType != serviceReturnType) {
        logger.warn().status(Status.REMIND)
          .log("Controller定义 [$interfaceType]，调用 Service方法 [$callMethodName(${interfaceMethod.parameterTypes})] 返回值不一致 - [$controllerReturnType, $serviceReturnType]")
        "return null;"
      } else {
        "return $serviceFieldName.$callMethodName($$);"
      }
    }
  }

  private fun setupDeclaredInterfaceMethodArguments(ctMethod: CtMethod) {
    val parameterNames: Array<String> = interfaceMethod.parameters.map { it.name }.toTypedArray()
    val parameterAttribute =
      MethodParametersAttribute(controllerMeta.constPool, parameterNames, IntArray(parameterNames.size) { 0 })
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 转换方法参数的注解
    val ctParameterAnnotationsAttribute =
      duplicateParametersAnnotations(interfaceMethod.parameters, controllerMeta.constPool)
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)
  }

  private fun duplicateParametersAnnotations(
    parameters: Array<Parameter>,
    constPool: ConstPool
  ): ParameterAnnotationsAttribute {
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<javassist.bytecode.annotation.Annotation>>()
    parameters.forEach { param ->
      val ctAnnotations = param.annotations.map { ia ->
        ControllerSourceGeneratorHelper.duplicateAnnotation(ia, controllerMeta.constPool)
      }
      ctParameterAnnotations.add(ctAnnotations.toTypedArray())
    }
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    return ctParameterAnnotationsAttribute
  }

  private fun copyDeclaredInterfaceMethodAnnotations(ctMethod: CtMethod) {
    val interfaceMethodAnnotations: Array<Annotation> = interfaceMethod.annotations.filter {
      // 对新生成的方法实现，去除掉Labzen的注解
      it.annotationClass.findAnnotations(LabzenWeb::class).isEmpty()
    }.toTypedArray()
    val ctClassAnnotationAttribute =
      ControllerSourceGeneratorHelper.duplicateAnnotations(interfaceMethodAnnotations, controllerMeta.constPool)
    val revisableAnnotations = ctClassAnnotationAttribute.annotations.toMutableList()

    if (controllerMeta.configuration.controllerVersionEnabled()) {
      val version = fetchVersion()
      ControllerMappingServiceVersionHelper.setupMappedRequestVersion(
        controllerMeta,
        version,
        revisableAnnotations
      )
    }

    ctClassAnnotationAttribute.annotations = revisableAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctClassAnnotationAttribute)
  }

  private fun fetchVersion(): Int? {
    if (!::callServiceMethod.isInitialized) {
      logger.warn().status(Status.REMIND)
        .log("Controller定义 [${controllerMeta.interfaceType}]，无法获取到准确的 API 版本定义")
      return null
    }

    return if (callServiceMethod.isAnnotationPresent(MappingServiceVersion::class.java)) {
      callServiceMethod.getAnnotation(MappingServiceVersion::class.java).value
    } else controllerMeta.apiVersion
  }

  companion object {
    private val logger = logger { }
  }
}