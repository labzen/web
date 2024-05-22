package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.source.ControllerMeta
import javassist.bytecode.annotation.Annotation
import java.lang.reflect.Method

internal class BaseResourceAllMethodGenerator(
  controllerMeta: ControllerMeta,
  serviceMethodNames: Array<String>,
  serviceFieldClass: Class<*>,
  serviceFieldName: String
) : AbstractBaseResourceMethodsGenerator(
  controllerMeta,
  serviceMethodNames,
  serviceFieldClass,
  serviceFieldName
) {

  override fun findServiceMethod(methodName: String): Method? =
    try {
      serviceFieldClass.getDeclaredMethod(methodName)
    } catch (e: NoSuchMethodException) {
      // todo 可配置日志是否打印
      logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
        .log("基于 @BaseResource 定义的方法 [${controllerMeta.interfaceType}] 无法找到对应的 Service 方法 [$serviceFieldClass#$methodName()]")
      null
    }

  override fun generateMethodSignature(methodName: String): String =
    "public Result allResources()"

  override fun generateMethodBody(serviceMethod: Method): String =
    "return $serviceFieldName.${serviceMethod.name}();"

  override fun setupMethod() {
    setupMethodAnnotations(
      arrayOf(Annotation("org.springframework.web.bind.annotation.GetMapping", controllerMeta.constPool))
    )
  }

  companion object {
    private val logger = logger { }
  }
}