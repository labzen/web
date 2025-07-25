package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 注解在 Controller 接口或方法上，处理（特定/所有）异常抛出时，定制有意义的响应数据
 *
 * Labzen Web 组件已经对所有的异常抛出进行了封装，统一了返回数据结构，响应内容默认是异常信息。
 *
 * 在声明本注解后，根据抛出的异常，返回有意义的响应数据。即在发生异常后，也可以返回有效数据
 *
 * todo 待实现
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Catching(
  // todo 改为接口，在接口中定义支持哪些异常，并处理返回数据
  val exceptions: Array<KClass<out Exception>>
)
