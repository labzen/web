package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.*;
import cn.labzen.web.apt.suggestion.AppendSuggestion;
import cn.labzen.web.apt.suggestion.RemoveSuggestion;
import cn.labzen.web.apt.suggestion.ReplaceSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.google.common.collect.Lists;
import com.squareup.javapoet.TypeName;
import jakarta.annotation.Resource;

import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.labzen.web.apt.definition.TypeNames.APT_ANNOTATION_CALL;

public final class CallEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_CALL).asType());
  }

  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    List<Suggestion> suggestions = Lists.newArrayList(new RemoveSuggestion(Utils.getSimpleName(supportedAnnotationType), ElementMethod.class));

    Object target = members.get("target");

    String fieldName = null;
    if (target instanceof TypeMirror tm) {
      TypeName fieldClass = Utils.typeOf(tm);
      fieldName = Utils.getSimpleName(fieldClass);
      fieldName = Strings.camelCase(fieldName);

      ElementAnnotation annotation = new ElementAnnotation(TypeName.get(Resource.class));
      ElementField elementField = new ElementField(fieldName, fieldClass, List.of(annotation));
      suggestions.add(new AppendSuggestion(elementField, ElementClass.class));
    }

    Object method = members.get("method");
    if (fieldName != null || method instanceof String) {
      ElementMethodBody body = new ElementMethodBody(Strings.value(fieldName, ""), Strings.value(method, ""), Collections.emptyList());
      suggestions.add(new ReplaceSuggestion(body.keyword(), body));
    }

    return suggestions;
  }
}
