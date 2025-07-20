package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口或方法上，对API响应内容进行缓存
 *
 * todo 待实现，定义缓存参数
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Caching()
