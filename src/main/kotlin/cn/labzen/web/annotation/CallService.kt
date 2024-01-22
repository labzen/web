package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 指定调用Service的哪一个方法，默认调用与Controller的方法相同名称的Service方法
 *
 * 默认调用的是[ServiceHandler.main]指向的Service，在这个注解中也可以使用[handler]指定调用其他Service，但其必须是在[ServiceHandler.services]中包含的
 *
 * - [method] 调用的方法名
 * - [handler] 调用的Service，必须要在[ServiceHandler]注解中生命过
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class CallService(
  val method: String,
  val handler: KClass<*> = Any::class
)
