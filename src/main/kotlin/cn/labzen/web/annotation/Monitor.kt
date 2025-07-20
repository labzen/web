package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口或方法上，对指定的API进行指标监控
 *
 * todo 待实现
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Monitor()
