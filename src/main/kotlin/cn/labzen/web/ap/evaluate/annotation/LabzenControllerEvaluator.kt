package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.annotation.LabzenController
import cn.labzen.web.ap.config.WebAPConfig
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import com.squareup.javapoet.TypeName

class LabzenControllerEvaluator : MethodErasableAnnotationEvaluator {

  override fun support(type: TypeName): Boolean =
    SUPPORTED == type

  override fun evaluate(config: WebAPConfig, type: TypeName, members: Map<String, Any?>): List<Suggestion> =
    listOf(RemoveSuggestion(Utils.getSimpleName(type), ElementClass::class.java))


  companion object {
    private val SUPPORTED = TypeName.get(LabzenController::class.java)
  }
}