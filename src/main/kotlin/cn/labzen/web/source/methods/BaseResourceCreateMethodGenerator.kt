package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.source.ControllerMeta
import javassist.bytecode.annotation.Annotation
import java.lang.reflect.Method

internal class BaseResourceCreateMethodGenerator(
  controllerMeta: ControllerMeta,
  serviceMethodNames: Array<String>,
  serviceFieldClass: Class<*>,
  serviceFieldName: String,
  resourceBeanClass: Class<*>,
) : AbstractBaseResourceMethodsGenerator(
  controllerMeta,
  serviceMethodNames,
  serviceFieldClass,
  serviceFieldName,
) {

  init {
    super.resourceBeanClass = resourceBeanClass
  }

  override fun findServiceMethod(methodName: String): Method? =
    try {
      serviceFieldClass.getDeclaredMethod(methodName, resourceBeanClass)
    } catch (e: NoSuchMethodException) {
      logger.warn().status(Status.FIXME)
        .log("Controller定义 [${controllerMeta.interfaceType}]，在指定的 Service [${serviceFieldClass}] 中找不到可调用的方法 - [$methodName(${resourceBeanClass.name} resource)]")
      null
    }

  override fun generateMethodSignature(methodName: String, version: String): String =
    "public Result createResource(${resourceBeanClass.name} resource)"

  override fun generateMethodBody(serviceMethod: Method): String =
    "return $serviceFieldName.${serviceMethod.name}(resource);"

  override fun setupMethod() {
    val constPool = controllerMeta.constPool

    setupMethodArguments(arrayOf("resource"))

    // 一个参数，Resource Bean，加注解 @Validated @RequestBody
    val argumentAnnotations = listOf(
      arrayOf(
        Annotation("org.springframework.validation.annotation.Validated", constPool),
        Annotation("org.springframework.web.bind.annotation.ModelAttribute", constPool)
      )
    )
    setupMethodArgumentAnnotations(argumentAnnotations)

    setupMethodAnnotations(arrayOf(Annotation("org.springframework.web.bind.annotation.PostMapping", constPool)))
  }

  companion object {
    private val logger = logger { }
  }
}