package cn.labzen.web.ap.suggestion;

public sealed interface Suggestion
  permits AppendSuggestion, DiscardSuggestion, RemoveSuggestion, ReplaceSuggestion {
}
