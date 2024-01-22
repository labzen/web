package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.source.ControllerMeta
import com.google.common.primitives.Primitives
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.StringMemberValue
import java.lang.reflect.Method

internal class BaseResourceInfoMethodGenerator(
  controllerMeta: ControllerMeta,
  serviceMethodNames: Array<String>,
  serviceFieldClass: Class<*>,
  serviceFieldName: String,
  resourceIdClass: Class<*>,
  resourceIdName: String
) : AbstractBaseResourceMethodsGenerator(
  controllerMeta,
  serviceMethodNames,
  serviceFieldClass,
  serviceFieldName
) {

  init {
    super.resourceIdClass = resourceIdClass
    super.resourceIdName = resourceIdName
  }

  private val parameterClass = if (resourceIdClass.isPrimitive)
    Primitives.wrap(resourceIdClass)
  else
    resourceIdClass

  override fun findServiceMethod(methodName: String): Method? =
    try {
      serviceFieldClass.getDeclaredMethod(methodName, parameterClass)
    } catch (e: NoSuchMethodException) {
      logger.warn().status(Status.FIXME)
        .log("Controller定义 [${controllerMeta.interfaceType}]，在指定的 Service [${serviceFieldClass}] 中找不到可调用的方法 - [$methodName(${parameterClass.simpleName} $resourceIdName)]")
      null
    }

  override fun generateMethodSignature(methodName: String, version: String): String =
    "public Result resourceInfo(${parameterClass.simpleName} $resourceIdName)"

  override fun generateMethodBody(serviceMethod: Method): String =
    "return $serviceFieldName.${serviceMethod.name}($resourceIdName);"

  override fun setupMethod() {
    val constPool = controllerMeta.constPool

    setupMethodArguments(arrayOf(resourceIdName))

    // 一个参数，Resource Bean，加注解 @PathVariable
    val argumentAnnotations = listOf(
      arrayOf(Annotation("org.springframework.web.bind.annotation.PathVariable", constPool))
    )
    setupMethodArgumentAnnotations(argumentAnnotations)

    val mappingAnnotation = Annotation("org.springframework.web.bind.annotation.GetMapping", constPool)
    val mappingAnnotationMember =
      ArrayMemberValue(constPool).apply { value = arrayOf(StringMemberValue("/{$resourceIdName}", constPool)) }
    mappingAnnotation.addMemberValue("value", mappingAnnotationMember)
    setupMethodAnnotations(arrayOf(mappingAnnotation))
  }

  companion object {
    private val logger = logger { }
  }
}