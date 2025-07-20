package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口方法上，调整接口的并发量域值相关参数
 *
 * todo 待实现
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Threshold()
