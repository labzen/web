package cn.labzen.web.ap.evaluate.generics

import cn.labzen.web.ap.suggestion.Suggestion
import com.squareup.javapoet.TypeName

/**
 * Controller 父接口泛型评价器
 */
interface InterfaceGenericsEvaluator {

  /**
   * 是否支持父接口
   */
  fun support(type: TypeName): Boolean

  /**
   * 根据父接口泛型定义做出代码生成建议
   *
   * @param arguments 父接口声明的泛型类型信息
   * @return 根据泛型对 Controller 实现类的建议
   */
  fun evaluate(arguments: List<TypeName>): List<Suggestion>
}