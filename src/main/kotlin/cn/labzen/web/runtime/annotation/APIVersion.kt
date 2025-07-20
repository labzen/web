package cn.labzen.web.runtime.annotation

import cn.labzen.web.defination.APIVersionCarrier

/**
 * 标识一个 Controller 方法的 API 请求版本，仅在使用 [APIVersionCarrier.URI] 方式控制 API 版本的情况下有效
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class APIVersion(
  val value: String
)