package cn.labzen.web.apt.suggestion;

import cn.labzen.web.apt.internal.element.Element;

public record RemoveSuggestion(String keyword, Class<? extends Element> kind) implements Suggestion {
}
