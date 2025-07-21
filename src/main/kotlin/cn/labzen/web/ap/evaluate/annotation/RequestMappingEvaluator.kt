package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementMethod
import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion
import cn.labzen.web.defination.APIVersionCarrier.*
import cn.labzen.web.runtime.annotation.APIVersion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.springframework.web.bind.annotation.RequestMapping

class RequestMappingEvaluator : MethodErasableAnnotationEvaluator {

  override fun support(type: TypeName): Boolean =
    type is ClassName && Utils.isRequestMappingAnnotation(type)

  override fun evaluate(config: Config, type: TypeName, members: Map<String, Any?>): List<Suggestion> {
    if (config.apiVersionCarrier() == DISABLE) {
      return emptyList()
    }

    val version = config.apiVersionPrefix() + config.apiVersionBased()
    val annotationSuggestion = when (config.apiVersionCarrier()) {
      URI -> versionByURI(version)
      HEADER -> versionByHeader(config, version, type)
      PARAMETER -> versionByParameter(config, version, type)
      else -> throw IllegalStateException("never happen")
    }

    return listOf(annotationSuggestion)
  }

  /**
   * 通过 URI 版本控制
   */
  private fun versionByURI(version: String): Suggestion {
    val annotation = ElementAnnotation(TypeName.get(APIVersion::class.java), mutableMapOf("value" to version))
    return AppendSuggestion(annotation, ElementMethod::class.java)
  }

  /**
   * 通过 Header Accept 版本控制
   */
  private fun versionByHeader(config: Config, version: String, clazz: TypeName): Suggestion {
    val headerVersion = "application/vnd.${config.apiVersionHeaderVND()}.$version+json"
    val annotation = ElementAnnotation(clazz, mutableMapOf("produces" to arrayOf(headerVersion)))
    return ReplaceSuggestion(SUPPORTED_NAME, annotation)
  }

  /**
   * 通过请求参数控制版本
   */
  private fun versionByParameter(config: Config, version: String, clazz: TypeName): Suggestion {
    val paramVersion = "${config.apiVersionParameterName()}=$version"
    val annotation = ElementAnnotation(clazz, mutableMapOf("params" to arrayOf(paramVersion)))
    return ReplaceSuggestion(SUPPORTED_NAME, annotation)
  }

  companion object {
    private val SUPPORTED = TypeName.get(RequestMapping::class.java)
    private val SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED)
  }
}