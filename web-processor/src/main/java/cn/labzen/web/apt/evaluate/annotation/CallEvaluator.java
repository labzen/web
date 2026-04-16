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

/**
 * @Call 注解评价器
 * <p>
 * 处理 @Call 注解，该注解用于声明接口方法调用其他服务的方法。
 * 评价结果为：
 * <ul>
 *   <li>移除 @Call 注解本身</li>
 *   <li>根据 target 属性添加服务层依赖注入字段</li>
 *   <li>根据 method 属性设置方法调用体</li>
 * </ul>
 */
public final class CallEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  /**
   * 初始化评价器，加载 @Call 注解类型
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_CALL).asType());
  }

  /**
   * 判断是否支持该注解类型
   *
   * @param type 注解类型
   * @return 是否支持
   */
  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  /**
   * 评价 @Call 注解，生成服务调用建议
   *
   * @param config 处理器配置
   * @param type 注解类型
   * @param members 注解成员值
   * @return 代码生成建议列表
   */
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
