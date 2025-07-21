package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.annotation.Call
import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.*
import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion
import com.squareup.javapoet.TypeName
import javax.annotation.Resource
import javax.lang.model.type.TypeMirror

class CallEvaluator : MethodErasableAnnotationEvaluator {

  override fun support(type: TypeName): Boolean =
    SUPPORTED == type

  override fun evaluate(config: Config, type: TypeName, members: Map<String, Any?>): List<Suggestion> {
    val suggestions =
      mutableListOf<Suggestion>(RemoveSuggestion(SUPPORTED_NAME, ElementMethod::class.java))

    val fieldClass = members["target"]?.let {
      Utils.typeOf(it as TypeMirror)
    }
    val fieldName = fieldClass?.let {
      val simpleName = Utils.getSimpleName(it)
      simpleName[0].lowercaseChar() + simpleName.substring(1)
    }
    fieldName?.run {
      val annotation = ElementAnnotation(TypeName.get(Resource::class.java))
      val field = ElementField(fieldName, fieldClass, listOf(annotation))
      suggestions.add(AppendSuggestion(field, ElementClass::class.java))
    }

    val methodName = members["method"] as String?
    if (fieldName != null || methodName != null) {
      val body = ElementMethodBody(fieldName ?: "", methodName ?: "", emptyList())
      suggestions.add(ReplaceSuggestion(body.keyword(), body))
    }

    return suggestions
  }

  companion object {
    private val SUPPORTED = TypeName.get(Call::class.java)
    private val SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED)
  }
}