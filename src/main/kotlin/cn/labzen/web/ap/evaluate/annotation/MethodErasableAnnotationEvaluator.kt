package cn.labzen.web.ap.evaluate.annotation

import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.suggestion.Suggestion
import com.squareup.javapoet.TypeName

/**
 * 方法注解集合评价器
 */
interface MethodErasableAnnotationEvaluator {

  /**
   * 是否支持父接口
   */
  fun support(type: TypeName): Boolean

  /**
   * 根据一个方法上的注解做出代码生成建议
   */
  fun evaluate(config: Config, type: TypeName, members: Map<String, Any?>): List<Suggestion>
}