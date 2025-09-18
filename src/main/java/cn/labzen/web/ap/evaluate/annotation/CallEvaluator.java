package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.annotation.Call;
import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.element.*;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.ReplaceSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.google.common.collect.Lists;
import com.squareup.javapoet.TypeName;
import jakarta.annotation.Resource;

import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CallEvaluator implements MethodErasableAnnotationEvaluator {

  private static final TypeName SUPPORTED = TypeName.get(Call.class);
  private static final String SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED);

  @Override
  public boolean support(TypeName type) {
    return SUPPORTED.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    List<Suggestion> suggestions = Lists.newArrayList(new RemoveSuggestion(SUPPORTED_NAME, ElementMethod.class));

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
