package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.request.PagingCondition
import cn.labzen.web.source.ControllerMeta
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.StringMemberValue
import java.lang.reflect.Method

internal class BaseResourceFindMethodGenerator(
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
      serviceFieldClass.getDeclaredMethod(methodName, resourceBeanClass, PagingCondition::class.java)
    } catch (e: NoSuchMethodException) {
      logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
        .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
        .log("基于 @BaseResource 定义的方法 [${controllerMeta.interfaceType}] 无法找到对应的 Service 方法 [$serviceFieldClass#$methodName(${resourceBeanClass.name} resourceCondition, cn.labzen.web.request.PagingCondition pageCondition)]")
      null
    }

  override fun generateMethodSignature(methodName: String): String =
    "public Result findResource(${resourceBeanClass.name} resourceCondition, PagingCondition pageCondition)"

  override fun generateMethodBody(serviceMethod: Method): String =
    "return $serviceFieldName.${serviceMethod.name}(resourceCondition, pageCondition);"

  override fun setupMethod() {
    val constPool = controllerMeta.constPool

    setupMethodArguments(arrayOf("resourceCondition", "pageCondition"))

    // 两个参数
    val modelAttributeAnnotation =
      Annotation("org.springframework.web.bind.annotation.ModelAttribute", constPool)
    val argumentAnnotations = listOf(
      arrayOf(modelAttributeAnnotation),
      arrayOf(modelAttributeAnnotation)
    )
    setupMethodArgumentAnnotations(argumentAnnotations)

    val mappingAnnotation = Annotation("org.springframework.web.bind.annotation.GetMapping", constPool)
    val mappingAnnotationMember =
      ArrayMemberValue(constPool).apply {
        value = arrayOf(StringMemberValue("/find", constPool))
      }
    mappingAnnotation.addMemberValue("value", mappingAnnotationMember)
    setupMethodAnnotations(arrayOf(mappingAnnotation))
  }

  companion object {
    private val logger = logger { }
  }
}