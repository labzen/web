package cn.labzen.web.response

/**
 * 分页信息
 */
data class Pagination(
  val page: Int,
  val size: Int,
  val recordCount: Long,
  val pageCount: Long
)