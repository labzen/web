package cn.labzen.web.annotation.runtime

import cn.labzen.web.annotation.LabzenWeb

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class APIVersion(
  val value: Int
)