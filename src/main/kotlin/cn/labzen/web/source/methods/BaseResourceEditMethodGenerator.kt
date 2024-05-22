package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.source.ControllerMeta
import javassist.bytecode.annotation.Annotation
import java.lang.reflect.Method

internal class BaseResourceEditMethodGenerator(
  controllerMeta: ControllerMeta,
  serviceMethodNames: Array<String>,
  serviceFieldClass: Class<*>,
  serviceFieldName: String,
  resourceBeanClass: Class<*>,
) : AbstractBaseResourceMethodsGenerator(
  controllerMeta,
  serviceMethodNames,
  serviceFieldClass,
  serviceFieldName
) {

  init {
    super.resourceBeanClass = resourceBeanClass
  }

  override fun findServiceMethod(methodName: String): Method? =
    try {
      serviceFieldClass.getDeclaredMethod(methodName, resourceBeanClass)
    } catch (e: NoSuchMethodException) {
      logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
        .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
        .log("基于 @BaseResource 定义的方法 [${controllerMeta.interfaceType}] 无法找到对应的 Service 方法 [$serviceFieldClass#$methodName(${resourceBeanClass.name} resource)]")
      null
    }

  override fun generateMethodSignature(methodName: String): String =
    "public Result editResource(${resourceBeanClass.name} resource)"

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

    setupMethodAnnotations(arrayOf(Annotation("org.springframework.web.bind.annotation.PutMapping", constPool)))
  }

  companion object {
    private val logger = logger { }
  }
}