package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

/**
 * 方法注解集合评价器
 */
public sealed interface MethodErasableAnnotationEvaluator permits AbandonedEvaluator, CallEvaluator, LabzenControllerEvaluator, MappingVersionEvaluator, RequestMappingEvaluator {

  /**
   * 是否支持父接口
   */
  boolean support(TypeName type);

  /**
   * 根据一个方法上的注解做出代码生成建议
   */
  List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members);
}
