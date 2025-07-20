package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口方法上，标识该方法弃用，将不再作为增删改查的 Restful API 入口
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Abandoned