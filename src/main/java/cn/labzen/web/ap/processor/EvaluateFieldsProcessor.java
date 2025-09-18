package cn.labzen.web.ap.processor;

import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.context.ControllerContext;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementClass;
import cn.labzen.web.ap.internal.element.ElementField;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.ReplaceSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

public final class EvaluateFieldsProcessor implements InternalProcessor {

  @Override
  public void process(ControllerContext context) {
    // 读取所有继承的父接口定义的泛型参数类型
    List<? extends TypeMirror> directSupertypes = context.getApc().getTypes().directSupertypes(context.getSource().asType());

    directSupertypes.forEach(inter -> {
      List<TypeName> typeArguments = detectTypeArguments(inter);
      if (typeArguments == null) return;

      TypeName interfaceClassName = Utils.typeOf(inter);

      // 遍历每一个评价器
      List<Suggestion> suggestions = context.getGenericsEvaluators().stream()
        .filter(evaluator -> evaluator.support(interfaceClassName))
        .flatMap(evaluator -> evaluator.evaluate(typeArguments).stream())
        .collect(Collectors.toList());

      suggestions.forEach(suggestion -> {
        switch (suggestion) {
          case AppendSuggestion append -> parseAppendSuggestion(context.getRoot(), append);
          case RemoveSuggestion remove -> parseRemoveSuggestion(context.getRoot(), remove);
          case ReplaceSuggestion replace -> parseReplaceSuggestion(context.getRoot(), replace);
          default -> throw new IllegalStateException("Unexpected suggestion: " + suggestion);
        }
      });
    });
  }

  private void parseAppendSuggestion(ElementClass root, AppendSuggestion suggestion) {
    if (suggestion.element() instanceof ElementField field) {
      root.getFields().add(field);
    } else if (suggestion.element() instanceof ElementAnnotation annotation) {
      if (ElementClass.class.equals(suggestion.kind())) {
        root.getAnnotations().add(annotation);
      }
    }
  }

  private void parseRemoveSuggestion(ElementClass root, RemoveSuggestion suggestion) {
    if (ElementClass.class.equals(suggestion.kind())) {
      return;
    }

    List<ElementField> needlessFields = root.getFields().stream().filter(filed -> filed.keyword().equals(suggestion.keyword())).toList();
    needlessFields.forEach(field -> root.getFields().remove(field));
    List<ElementAnnotation> needlessAnnotations = root.getAnnotations().stream().filter(ann -> ann.keyword().equals(suggestion.keyword())).toList();
    needlessAnnotations.forEach(field -> root.getAnnotations().remove(field));
  }

  private void parseReplaceSuggestion(ElementClass root, ReplaceSuggestion suggestion) {
    if (suggestion.element() instanceof ElementAnnotation annotation) {
      List<ElementAnnotation> annotations = root.getAnnotations().stream().filter(ann -> ann.keyword().equals(suggestion.keyword())).toList();
      annotations.forEach(ann -> ann.getMembers().putAll(annotation.getMembers()));
    }
  }

  private List<TypeName> detectTypeArguments(TypeMirror typeMirror) {
    if (typeMirror instanceof DeclaredType declaredType) {
      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      return typeArguments.stream().map(Utils::typeOf).toList();
    } else {
      return null;
    }
  }

  @Override
  public int priority() {
    return PRIORITY_EVALUATE_FIELDS;
  }
}
