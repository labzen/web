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

/**
 * @Abandoned 注解评价器
 * <p>
 * 处理 @Abandoned 注解，该注解用于标记已废弃的接口方法。
 * 评价结果为：移除 @Abandoned 注解本身，并设置方法为废弃状态。
 */
public final class AbandonedEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;

  /**
   * 初始化评价器，加载 @Abandoned 注解类型
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_ABANDONED).asType());
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
   * 评价 @Abandoned 注解，生成废弃方法建议
   *
   * @param config 处理器配置
   * @param type 注解类型
   * @param members 注解成员值
   * @return 代码生成建议列表
   */
  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    RemoveSuggestion removeSuggestion = new RemoveSuggestion(Utils.getSimpleName(type), ElementMethod.class);
    DiscardSuggestion discardSuggestion = new DiscardSuggestion();
    return List.of(removeSuggestion, discardSuggestion);
  }
}
