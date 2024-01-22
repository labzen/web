package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 指向处理业务逻辑的service层业务类，所有的 Service 类实例都会被注入 Controller 中
 *
 * - [main] 在 controller 中将调用的主要 Service
 * - [services] 可指定多个需要注入的 Service，可使用 [CallService] 来指定使用哪个 Service 处理业务逻辑
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class ServiceHandler(
  val main: KClass<*>,
  val services: Array<KClass<*>> = []
)
