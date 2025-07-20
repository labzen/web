package cn.labzen.web.ap.suggestion.impl

import cn.labzen.web.ap.internal.element.Element
import cn.labzen.web.ap.suggestion.Suggestion

data class AppendSuggestion(
  val element: Element,
  val kind: Class<out Element>
) : Suggestion