package cn.labzen.web.response

/**
 * 分页信息
 *
 * @property page 当前页
 * @property size 每页记录数
 * @property recordCount 记录总数
 * @property pageCount 总页数
 */
@Deprecated("")
data class Pagination(
  val page: Int,
  val size: Int,
  val recordCount: Long,
  val pageCount: Long
) {

  companion object {
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> from(page: T): Pagination? =
      (converter as? PaginationConverter<T>)?.convert(page)

    internal var converter: PaginationConverter<*>? = null
  }
}