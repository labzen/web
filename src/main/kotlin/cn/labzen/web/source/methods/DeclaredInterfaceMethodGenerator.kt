package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.annotation.CallService
import cn.labzen.web.annotation.LabzenWeb
import cn.labzen.web.annotation.MappingVersion
import cn.labzen.web.source.ControllerMappingVersionHelper
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

/**
 * Controller 接口定义方法生成器
 */
internal class DeclaredInterfaceMethodGenerator(
  private val controllerMeta: ControllerMeta,
  private val interfaceMethod: Method
) {

  private lateinit var callServiceClass: Class<*>
  private lateinit var callMethodName: String
  private lateinit var callServiceMethod: Method

  fun generate() {
    // 定义的方法参数集合（Type name）
    val methodParameterNames = interfaceMethod.parameters.joinToString(",") {
      "${it.type.name} ${it.name}"
    }
    // 方法签名
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

  /**
   * 根据[CallService]确定Controller接口定义的方法，将会调用的Service
   */
  private fun findServiceTarget() {
    if (interfaceMethod.isAnnotationPresent(CallService::class.java)) {
      val annotation = interfaceMethod.getAnnotation(CallService::class.java)

      // 调用的service方法名，为空使用Controller方法名
      callMethodName = annotation.method.ifBlank { interfaceMethod.name }

      // 没有特殊指定Service类，使用ServiceHandler.main指定的主Service
      callServiceClass = if (annotation.handler.java == Any::class.java) {
        controllerMeta.services[controllerMeta.mainServiceFieldName]!!
      } else
        annotation.handler.java
    } else {
      // 方法上没有注解CallService的话，使用Controller接口上使用ServiceHandler.main指定的主Service
      callMethodName = interfaceMethod.name
      callServiceClass = controllerMeta.services[controllerMeta.mainServiceFieldName]!!
    }
  }

  /**
   * 对Controller接口中定义的方法，生成有意义的方法体
   */
  private fun generateDeclaredInterfaceMethodBody(): String {
    findServiceTarget()

    with(controllerMeta) {
      val controllerReturnType = interfaceMethod.returnType

      // 如果方法的返回为 void
      if (controllerReturnType == Void::class.java) {
        logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.REMIND)
          .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
          .log("[$interfaceType#${interfaceMethod.name}(${interfaceMethod.parameterTypes})] 未提供返回值")
        return ""
      }

      callServiceMethod = try {
        callServiceClass.getDeclaredMethod(callMethodName, *interfaceMethod.parameterTypes)
      } catch (e: NoSuchMethodException) {
        logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
          .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
          .log("[$interfaceType#${interfaceMethod.name}(${interfaceMethod.parameterTypes})] 需要调用的方法不存在 [$callServiceClass#$callMethodName(${interfaceMethod.parameterTypes})]")
        return "return null;"
      }

      val serviceFieldName = services.inverse()[callServiceClass]
      val serviceReturnType = callServiceMethod.returnType

      return when {
        // Service方法没有返回值
        serviceReturnType == Void::class.java -> {
          logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
            .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
            .log("[$callServiceClass#$callMethodName(${interfaceMethod.parameterTypes})] 未提供返回值")
          "return null;"
        }
        // Service方法的返回类型与Controller方法的返回类型不一致，或Service方法的返回类型不是Controller方法的返回类型的子类或接口实现类
        !controllerReturnType.isAssignableFrom(serviceReturnType) -> {
          logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
            .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
            .log("[$callServiceClass#$callMethodName(${interfaceMethod.parameterTypes})] 的返回类型 $serviceReturnType，无法匹配 [$controllerReturnType $interfaceType#${interfaceMethod.name}(${interfaceMethod.parameterTypes})]")
          "return null;"
        }
        // 调用Service方法
        else -> "return $serviceFieldName.$callMethodName($$);"
      }
    }
  }

  /**
   * 设置方法参数的注解，以及名称
   */
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

  /**
   * 复制参数的注解
   */
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

  /**
   * 复制方法上的注解
   */
  private fun copyDeclaredInterfaceMethodAnnotations(ctMethod: CtMethod) {
    val interfaceMethodAnnotations: Array<Annotation> = interfaceMethod.annotations.filter {
      // 对新生成的方法实现，排除掉Labzen的注解
      it.annotationClass.findAnnotations(LabzenWeb::class).isEmpty()
    }.toTypedArray()
    val ctClassAnnotationAttribute =
      ControllerSourceGeneratorHelper.duplicateAnnotations(interfaceMethodAnnotations, controllerMeta.constPool)
    val revisableAnnotations = ctClassAnnotationAttribute.annotations.toMutableList()

    // 设置API版本
    if (controllerMeta.configuration.controllerVersionEnabled()) {
      val version = fetchVersion()
      ControllerMappingVersionHelper.setupMappedRequestVersion(
        controllerMeta,
        version,
        revisableAnnotations
      )
    }

    ctClassAnnotationAttribute.annotations = revisableAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctClassAnnotationAttribute)
  }

  /**
   * 获取方法的API版本号
   */
  private fun fetchVersion(): Int? {
    return if (interfaceMethod.isAnnotationPresent(MappingVersion::class.java)) {
      interfaceMethod.getAnnotation(MappingVersion::class.java).value
    } else controllerMeta.apiVersion
  }

  companion object {
    private val logger = logger { }
  }
}