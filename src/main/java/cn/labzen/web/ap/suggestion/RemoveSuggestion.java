package cn.labzen.web.ap.suggestion;

import cn.labzen.web.ap.internal.element.Element;

public record RemoveSuggestion(String keyword, Class<? extends Element> kind) implements Suggestion {
}
