package cn.labzen.web.spring.runtime

import cn.labzen.meta.Labzens
import cn.labzen.web.annotation.runtime.APIVersion
import cn.labzen.web.meta.WebConfiguration
import org.springframework.context.EmbeddedValueResolverAware
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.StringValueResolver
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * 自定义通过 URI 控制API版本的 [RequestMappingHandlerMapping]
 */
class ApiVersionRequestMappingHandlerMapping : RequestMappingHandlerMapping(), EmbeddedValueResolverAware {

  private val versionPrefix = Labzens.configurationWith(WebConfiguration::class.java).controllerVersionPrefix()

  private var embeddedValueResolver: StringValueResolver? = null

  override fun setEmbeddedValueResolver(resolver: StringValueResolver) {
    this.embeddedValueResolver = resolver
    super.setEmbeddedValueResolver(resolver)
  }

  override fun getMappingForMethod(method: Method, handlerType: Class<*>): RequestMappingInfo? {
    var info = createRequestMappingInfo(method) ?: return null

    val typeInfo = createRequestMappingInfo(handlerType)
    if (typeInfo != null) {
      info = typeInfo.combine(info)
    }

    val mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, APIVersion::class.java)
    if (mappingAnnotation != null) {
      val version = versionPrefix + mappingAnnotation.value
      info = RequestMappingInfo.paths(version).options(builderConfiguration).build().combine(info)
    }

    val prefix = getPathPrefix(handlerType)
    if (prefix != null) {
      info = RequestMappingInfo.paths(prefix).options(builderConfiguration).build().combine(info)
    }
    return info
  }

  private fun createRequestMappingInfo(element: AnnotatedElement): RequestMappingInfo? {
    val requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping::class.java)
    val condition = if (element is Class<*>)
      getCustomTypeCondition(element)
    else
      getCustomMethodCondition(element as Method)

    return if (requestMapping != null)
      super.createRequestMappingInfo(requestMapping, condition)
    else null
  }

  private fun getPathPrefix(handlerType: Class<*>?): String? {
    for (entry in pathPrefixes.entries) {
      if (entry.value.test(handlerType)) {
        var prefix = entry.key
        if (embeddedValueResolver != null) {
          prefix = embeddedValueResolver!!.resolveStringValue(prefix!!)
        }
        return prefix
      }
    }
    return null
  }
}