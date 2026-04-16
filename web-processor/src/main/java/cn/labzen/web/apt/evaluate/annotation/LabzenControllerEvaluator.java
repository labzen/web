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

/**
 * @LabzenController 注解评价器
 * <p>
 * 处理 Controller 接口类上的 @LabzenController 注解。
 * 该注解仅用于标记，不应出现在生成的实现类中，因此评价结果为移除该注解。
 */
public final class LabzenControllerEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  /**
   * 初始化评价器，加载 @LabzenController 注解类型
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_LABZEN_CONTROLLER).asType());
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
   * 评价 @LabzenController 注解，生成移除建议
   *
   * @param config 处理器配置
   * @param type 注解类型
   * @param members 注解成员值
   * @return 代码生成建议列表
   */
  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    return List.of(new RemoveSuggestion(Utils.getSimpleName(type), ElementClass.class));
  }
}
