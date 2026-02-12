package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.ElementMethod;
import cn.labzen.web.apt.suggestion.DiscardSuggestion;
import cn.labzen.web.apt.suggestion.RemoveSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import static cn.labzen.web.apt.definition.TypeNames.APT_ANNOTATION_ABANDONED;

public final class AbandonedEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_ABANDONED).asType());
  }

  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    RemoveSuggestion removeSuggestion = new RemoveSuggestion(Utils.getSimpleName(type), ElementMethod.class);
    DiscardSuggestion discardSuggestion = new DiscardSuggestion();
    return List.of(removeSuggestion, discardSuggestion);
  }
}
