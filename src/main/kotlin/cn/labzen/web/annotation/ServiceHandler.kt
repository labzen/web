package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 指向处理业务逻辑的service层业务类，类实例都会被注入 Controller 中
 *
 * - [value] 在 controller 中将调用的主要 Service
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class ServiceHandler(
  val value: KClass<*>
)
