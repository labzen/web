package cn.labzen.web.annotation

import org.springframework.web.bind.annotation.RequestMapping

/**
 * 提供定义 [RequestMapping] 接口的版本，注解在通过 [ServiceHandler] 定义的 service 类（或方法）上
 *
 * - [value] 版本号
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class MappingServiceVersion(
  val value: Int
)
