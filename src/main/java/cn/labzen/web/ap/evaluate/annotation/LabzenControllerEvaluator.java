package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.internal.element.ElementClass;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import static cn.labzen.web.ap.definition.TypeNames.AP_ANNOTATION_LABZEN_CONTROLLER;

public final class LabzenControllerEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(AP_ANNOTATION_LABZEN_CONTROLLER).asType());
  }

  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    return List.of(new RemoveSuggestion(Utils.getSimpleName(type), ElementClass.class));
  }
}
