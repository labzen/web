package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.annotation.Abandoned
import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.ElementMethod
import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.ap.suggestion.impl.DiscardSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import com.squareup.javapoet.TypeName

class AbandonedEvaluator : MethodErasableAnnotationEvaluator {

  override fun support(type: TypeName): Boolean =
    SUPPORTED == type

  override fun evaluate(config: Config, type: TypeName, members: Map<String, Any?>): List<Suggestion> =
    listOf(RemoveSuggestion(Utils.getSimpleName(type), ElementMethod::class.java), DiscardSuggestion())

  companion object {
    private val SUPPORTED = TypeName.get(Abandoned::class.java)
  }
}