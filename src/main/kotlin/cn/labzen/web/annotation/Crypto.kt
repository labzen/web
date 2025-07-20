package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口或方法上，对API响应数据进行加密
 *
 * todo 待实现
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Crypto()
