package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

/**
 * 方法注解集合评价器
 */
public sealed interface MethodAnnotationErasableEvaluator permits AbandonedEvaluator, CallEvaluator, LabzenControllerEvaluator, MappingVersionEvaluator, RequestMappingEvaluator {

  /**
   * 初始化
   */
  void init(AnnotationProcessorContext context);

  /**
   * 是否支持父接口
   */
  boolean support(TypeName type);

  /**
   * 根据一个方法上的注解做出代码生成建议
   */
  List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members);
}
