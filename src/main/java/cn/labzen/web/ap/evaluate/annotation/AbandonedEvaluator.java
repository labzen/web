package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.annotation.Abandoned;
import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.element.ElementMethod;
import cn.labzen.web.ap.suggestion.DiscardSuggestion;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

public final class AbandonedEvaluator implements MethodErasableAnnotationEvaluator {

  private static final TypeName SUPPORTED = TypeName.get(Abandoned.class);

  @Override
  public boolean support(TypeName type) {
    return SUPPORTED.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    RemoveSuggestion removeSuggestion = new RemoveSuggestion(Utils.getSimpleName(type), ElementMethod.class);
    DiscardSuggestion discardSuggestion = new DiscardSuggestion();
    return List.of(removeSuggestion, discardSuggestion);
  }
}
