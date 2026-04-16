package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementClass;
import cn.labzen.web.apt.internal.element.ElementField;
import cn.labzen.web.apt.suggestion.*;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价 Controller 父接口的泛型参数
 * <p>
 * 分析 Controller 继承的父接口（如 StandardController&lt;S, T, ID>）的泛型类型参数，
 * 根据泛型类型生成服务层依赖注入字段的建议。
 */
public final class EvaluateFieldsProcessor implements InternalProcessor {

  /**
   * 处理父接口泛型参数，生成字段建议
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    // 读取所有继承的父接口定义的泛型参数类型
    List<? extends TypeMirror> directSupertypes = context.getApc().types().directSupertypes(context.getSource().asType());

    directSupertypes.forEach(inter -> {
      List<TypeName> typeArguments = detectTypeArguments(inter);
      if (typeArguments == null || typeArguments.isEmpty()) return;

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
          case DiscardSuggestion ignored -> {/*ignore that*/}
          default -> throw new IllegalStateException("Unexpected suggestion: " + suggestion);
        }
      });
    });
  }

  /**
   * 解析追加建议，添加新字段或注解
   *
   * @param root 根节点
   * @param suggestion 追加建议
   */
  private void parseAppendSuggestion(ElementClass root, AppendSuggestion suggestion) {
    if (suggestion.element() instanceof ElementField field) {
      root.getFields().add(field);
    } else if (suggestion.element() instanceof ElementAnnotation annotation) {
      if (ElementClass.class.equals(suggestion.kind())) {
        root.getAnnotations().add(annotation);
      }
    }
  }

  /**
   * 解析移除建议，删除指定字段
   *
   * @param root 根节点
   * @param suggestion 移除建议
   */
  private void parseRemoveSuggestion(ElementClass root, RemoveSuggestion suggestion) {
    if (ElementClass.class.equals(suggestion.kind())) {
      return;
    }

    List<ElementField> needlessFields = root.getFields().stream().filter(field -> field.keyword().equals(suggestion.keyword())).toList();
    needlessFields.forEach(field -> root.getFields().remove(field));
    List<ElementAnnotation> needlessAnnotations = root.getAnnotations().stream().filter(ann -> ann.keyword().equals(suggestion.keyword())).toList();
    needlessAnnotations.forEach(field -> root.getAnnotations().remove(field));
  }

  /**
   * 解析替换建议，修改注解属性
   *
   * @param root 根节点
   * @param suggestion 替换建议
   */
  private void parseReplaceSuggestion(ElementClass root, ReplaceSuggestion suggestion) {
    if (suggestion.element() instanceof ElementAnnotation annotation) {
      List<ElementAnnotation> annotations = root.getAnnotations().stream().filter(ann -> ann.keyword().equals(suggestion.keyword())).toList();
      annotations.forEach(ann -> ann.getMembers().putAll(annotation.getMembers()));
    }
  }

  /**
   * 从类型镜像中提取泛型类型参数列表
   *
   * @param typeMirror 类型镜像
   * @return 泛型类型参数列表，如果无泛型参数则返回 null
   */
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
