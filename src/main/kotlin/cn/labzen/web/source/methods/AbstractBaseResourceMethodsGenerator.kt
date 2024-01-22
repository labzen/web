package cn.labzen.web.source.methods

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.annotation.MappingServiceVersion
import cn.labzen.web.response.result.Result
import cn.labzen.web.source.ControllerMappingServiceVersionHelper
import cn.labzen.web.source.ControllerMeta
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.MethodParametersAttribute
import javassist.bytecode.ParameterAnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import java.lang.reflect.Method

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
  private var latestServiceVersion: Int? = null


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
      logger.warn().status(Status.FIXME)
        .log("Controller定义 [${controllerMeta.interfaceType}]，指定的 Service方法 [${serviceMethod}] 返回值需要是[${Result::class.java}]")
      return null
    }

    latestServiceVersion = fetchServiceMethodVersion(serviceMethod)
    val versionName = latestServiceVersion?.let { "V$it" } ?: ""

    val signature = generateMethodSignature(methodName, versionName)
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

  abstract fun findServiceMethod(methodName: String): Method?

  private fun fetchServiceMethodVersion(method: Method): Int? {
    return if (method.isAnnotationPresent(MappingServiceVersion::class.java)) {
      method.getAnnotation(MappingServiceVersion::class.java).value
    } else controllerMeta.apiVersion
  }

  abstract fun generateMethodSignature(methodName: String, version: String): String

  abstract fun generateMethodBody(serviceMethod: Method): String

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
    ControllerMappingServiceVersionHelper.setupMappedRequestVersion(controllerMeta, latestServiceVersion, revisableAnnotations)

    val annotationsAttribute = AnnotationsAttribute(controllerMeta.constPool, AnnotationsAttribute.visibleTag)
    annotationsAttribute.annotations = revisableAnnotations.toTypedArray()

    latestGeneratedMethod.methodInfo.addAttribute(annotationsAttribute)
  }

  companion object {
    private val logger = logger { }
  }
}