package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 指向处理业务逻辑的service层业务类
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class ServiceHandler(
  val main: KClass<*>,
  val services: Array<KClass<*>> = []
)
