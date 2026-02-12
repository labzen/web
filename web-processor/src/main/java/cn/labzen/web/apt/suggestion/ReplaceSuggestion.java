package cn.labzen.web.apt.suggestion;

import cn.labzen.web.apt.internal.element.Element;

public record ReplaceSuggestion(String keyword, Element element) implements Suggestion {
}
