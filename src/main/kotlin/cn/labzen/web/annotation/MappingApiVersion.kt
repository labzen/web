package cn.labzen.web.annotation

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class MappingApiVersion(
  val value: Int
)