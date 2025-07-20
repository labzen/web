package cn.labzen.web.ap.suggestion.impl

import cn.labzen.web.ap.internal.element.Element
import cn.labzen.web.ap.suggestion.Suggestion

data class RemoveSuggestion(
  val keyword: String,
  val kind: Class<out Element>
) : Suggestion