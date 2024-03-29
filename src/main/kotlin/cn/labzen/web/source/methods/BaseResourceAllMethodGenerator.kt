package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
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
      logger.warn().status(Status.FIXME)
        .log("Controller定义 [${controllerMeta.interfaceType}]，在指定的 Service [${serviceFieldClass}] 中找不到可调用的方法 - [$methodName()]")
      null
    }

  override fun generateMethodSignature(methodName: String, version: String): String =
    "public Result resource$methodName$version()"

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