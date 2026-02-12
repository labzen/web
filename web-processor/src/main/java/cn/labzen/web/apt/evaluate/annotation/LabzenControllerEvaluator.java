package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.ElementClass;
import cn.labzen.web.apt.suggestion.RemoveSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import static cn.labzen.web.apt.definition.TypeNames.APT_ANNOTATION_LABZEN_CONTROLLER;

public final class LabzenControllerEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_LABZEN_CONTROLLER).asType());
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
