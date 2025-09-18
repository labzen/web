package cn.labzen.web.ap.suggestion;

import cn.labzen.web.ap.internal.element.Element;

public record ReplaceSuggestion(String keyword, Element element) implements Suggestion {
}
