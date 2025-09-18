package cn.labzen.web.ap.suggestion;

import cn.labzen.web.ap.internal.element.Element;

public record AppendSuggestion(Element element, Class<? extends Element> kind) implements Suggestion {
}
