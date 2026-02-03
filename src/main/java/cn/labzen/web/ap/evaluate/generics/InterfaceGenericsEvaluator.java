package cn.labzen.web.ap.evaluate.generics;

import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;

/**
 * Controller 父接口泛型评价器
 */
public sealed interface InterfaceGenericsEvaluator permits PrimaryServiceGenericsAssign {

  /**
   * 初始化
   */
  void init(AnnotationProcessorContext context);

  /**
   * 是否支持父接口
   */
  boolean support(TypeName type);

  /**
   * 根据父接口泛型定义做出代码生成建议
   *
   * @param arguments 父接口声明的泛型类型信息
   * @return 根据泛型对 Controller 实现类的建议
   */
  List<? extends Suggestion> evaluate(List<TypeName> arguments);
}
