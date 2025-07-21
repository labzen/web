package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.annotation.MappingVersion
import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementMethod
import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion
import cn.labzen.web.defination.APIVersionCarrier.*
import cn.labzen.web.runtime.annotation.APIVersion
import com.squareup.javapoet.TypeName
import org.springframework.web.bind.annotation.RequestMapping

class MappingVersionEvaluator : MethodErasableAnnotationEvaluator {

  override fun support(type: TypeName): Boolean =
    SUPPORTED == type

  override fun evaluate(config: Config, type: TypeName, members: Map<String, Any?>): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>(RemoveSuggestion(SUPPORTED_NAME, ElementMethod::class.java))

    if (config.apiVersionCarrier() == DISABLE) {
      return suggestions
    }

    val version = config.apiVersionPrefix() + members.values.first()!!

    val annotationSuggestion = when (config.apiVersionCarrier()) {
      URI -> versionByURI(version)
      HEADER -> versionByHeader(config, version)
      PARAMETER -> versionByParameter(config, version)
      else -> throw IllegalStateException("never happen")
    }
    suggestions.add(annotationSuggestion)

    return suggestions
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
  private fun versionByHeader(config: Config, version: String): Suggestion {
    val headerVersion = "application/vnd.${config.apiVersionHeaderVND()}.$version+json"
    val annotation = ElementAnnotation(MAPPING_TYPE, mutableMapOf("produces" to arrayOf(headerVersion)))
    return ReplaceSuggestion(MAPPING_TYPE_NAME, annotation)
  }

  /**
   * 通过请求参数控制版本
   */
  private fun versionByParameter(config: Config, version: String): Suggestion {
    val paramVersion = "${config.apiVersionParameterName()}=$version"
    val annotation = ElementAnnotation(MAPPING_TYPE, mutableMapOf("params" to arrayOf(paramVersion)))
    return ReplaceSuggestion(MAPPING_TYPE_NAME, annotation)
  }

  companion object {
    private val SUPPORTED = TypeName.get(MappingVersion::class.java)
    private val SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED)
    private val MAPPING_TYPE = TypeName.get(RequestMapping::class.java)
    private val MAPPING_TYPE_NAME = Utils.getSimpleName(MAPPING_TYPE)
  }
}
