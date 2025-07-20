package cn.labzen.web.spring.runtime

import cn.labzen.web.defination.APIVersionCarrier
import cn.labzen.web.runtime.annotation.APIVersion
import org.springframework.context.EmbeddedValueResolverAware
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.StringValueResolver
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * 自定义 [RequestMappingHandlerMapping] ，以适配通过 [APIVersionCarrier.URI] 方式控制API版本的请求映射
 *
 * 仅当 `labzen.yml` 的配置项 `api-version.carrier = URI` 时生效
 */
class LabzenVersionedApiRequestMappingHandlerMapping : RequestMappingHandlerMapping(), EmbeddedValueResolverAware {

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

    // 从这里开始才是重点
    val versionedMappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, APIVersion::class.java)
    // 如果包含APIVersion注解，才会在映射路径中加入版本标识
    if (versionedMappingAnnotation != null) {
      val version = versionedMappingAnnotation.value
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