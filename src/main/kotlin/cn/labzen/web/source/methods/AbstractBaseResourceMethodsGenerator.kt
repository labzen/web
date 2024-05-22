package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.response.result.Result
import cn.labzen.web.source.ControllerMappingVersionHelper
import cn.labzen.web.source.ControllerMeta
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.MethodParametersAttribute
import javassist.bytecode.ParameterAnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import java.lang.reflect.Method

/**
 * 提供注解了 [BaseResource] 的Controller，常用的API方法
 */
internal abstract class AbstractBaseResourceMethodsGenerator(
  protected val controllerMeta: ControllerMeta,
  private val serviceMethodNames: Array<String>,
  protected val serviceFieldClass: Class<*>,
  protected val serviceFieldName: String,
) {

  protected lateinit var resourceBeanClass: Class<*>
  protected lateinit var resourceIdClass: Class<*>
  protected lateinit var resourceIdName: String

  private lateinit var latestGeneratedMethod: CtMethod
  private var baseApiVersion: Int? = controllerMeta.apiVersion

  fun generate() {
    serviceMethodNames.toSet().forEach {
      generateMethod(it)?.apply {
        controllerMeta.clazz.addMethod(this)
      }
    }
  }

  private fun generateMethod(methodName: String): CtMethod? {
    val serviceMethod = findServiceMethod(methodName) ?: return null

    if (serviceMethod.returnType != Result::class.java) {
      logger.warn().scene(LOGGER_SCENE_CONTROLLER).status(Status.FIXME)
        .conditional(!controllerMeta.configuration.ignoreControllerSourceWarning())
        .log("基于 @BaseResource 定义的方法 [${controllerMeta.interfaceType}] 将要调用的Service方法 [${serviceMethod}#$methodName()] 返回类型需要是 [${Result::class.java}]")
      return null
    }

    val signature = generateMethodSignature(methodName)
    val body = generateMethodBody(serviceMethod)
    // 创建方法
    latestGeneratedMethod = CtNewMethod.make(
      """
      $signature {
        $body
      }
    """.trimIndent(), controllerMeta.clazz
    )

    setupMethod()

    return latestGeneratedMethod
  }

  /**
   * 寻找Service中的对应方法
   */
  abstract fun findServiceMethod(methodName: String): Method?

  /**
   * 生成方法签名
   */
  abstract fun generateMethodSignature(methodName: String): String

  /**
   * 生成方法体实现
   */
  abstract fun generateMethodBody(serviceMethod: Method): String

  /**
   * 设置方法（注解等）
   */
  abstract fun setupMethod()

  protected fun setupMethodArguments(names: Array<String>) {
    val parameterAttribute = MethodParametersAttribute(controllerMeta.constPool, names, IntArray(names.size) { 0 })
    latestGeneratedMethod.methodInfo.addAttribute(parameterAttribute)
  }

  protected fun setupMethodArgumentAnnotations(argumentAnnotations: List<Array<Annotation>>) {
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(controllerMeta.constPool, ParameterAnnotationsAttribute.visibleTag)
    ctParameterAnnotationsAttribute.annotations = argumentAnnotations.toTypedArray()
    latestGeneratedMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)
  }

  protected fun setupMethodAnnotations(annotations: Array<Annotation>) {
    val revisableAnnotations = annotations.toMutableList()
    ControllerMappingVersionHelper.setupMappedRequestVersion(
      controllerMeta, baseApiVersion, revisableAnnotations
    )

    val annotationsAttribute = AnnotationsAttribute(controllerMeta.constPool, AnnotationsAttribute.visibleTag)
    annotationsAttribute.annotations = revisableAnnotations.toTypedArray()

    latestGeneratedMethod.methodInfo.addAttribute(annotationsAttribute)
  }

  companion object {
    private val logger = logger { }
  }
}