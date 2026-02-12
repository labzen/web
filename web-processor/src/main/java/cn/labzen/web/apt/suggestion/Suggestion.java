package cn.labzen.web.apt.suggestion;

public sealed interface Suggestion
  permits AppendSuggestion, DiscardSuggestion, RemoveSuggestion, ReplaceSuggestion {
}
