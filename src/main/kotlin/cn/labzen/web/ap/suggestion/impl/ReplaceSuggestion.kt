package cn.labzen.web.ap.suggestion.impl

import cn.labzen.web.ap.internal.element.Element
import cn.labzen.web.ap.suggestion.Suggestion

class ReplaceSuggestion(
  val keyword: String,
  val element: Element,
) : Suggestion