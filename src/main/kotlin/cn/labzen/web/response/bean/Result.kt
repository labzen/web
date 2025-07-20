package cn.labzen.web.response.bean

/**
 * Labzen Web 组件标准响应返回信息
 */
data class Result(
  val code: Int,
  val value: Any? = null,
  val message: String? = null,
)