package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 指定调用Service的哪一个方法，默认调用与Controller的方法相同名称的Service方法
 *
 * 默认调用的是[ServiceHandler.value]指向的Service，在这个注解中也可以使用[handler]指定调用其他Service
 *
 * - [method] 调用的方法名，如为空，默认调用与Controller方法相同的方法
 * - [handler] 调用的Service，如果与[ServiceHandler.value]不一致的话
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class CallService(
  val method: String = "",
  val handler: KClass<*> = Any::class
)
