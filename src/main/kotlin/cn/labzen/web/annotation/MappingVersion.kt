package cn.labzen.web.annotation

import org.springframework.web.bind.annotation.RequestMapping

/**
 * 提供定义 [RequestMapping] 接口的版本，与 [RequestMapping] 一样注解Controller方法上
 *
 * - [value] 版本号
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class MappingVersion(
  val value: Int
)
