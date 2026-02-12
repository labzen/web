package cn.labzen.web.apt.suggestion;

import cn.labzen.web.apt.internal.element.Element;

public record AppendSuggestion(Element element, Class<? extends Element> kind) implements Suggestion {
}
