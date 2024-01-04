package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 映射业务逻辑处理所产生的异常，以什么格式、内容输出
 * // todo 对未处理的异常，做默认页面或rest结构处理
 */
annotation class Catching(
  val exceptions: Array<KClass<out Exception>>
)
