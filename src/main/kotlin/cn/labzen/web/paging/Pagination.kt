package cn.labzen.web.paging

import cn.labzen.web.paging.converter.PageConverterHolder

/**
 * 分页结果信息
 *
 * @property pageNumber 当前页
 * @property pageSize 每页记录数
 * @property totalRecords 记录总数
 * @property totalPages 总页数
 * @property records 查询记录结果
 */
data class Pagination<R>(
  val pageNumber: Int,
  val pageSize: Int,
  val totalRecords: Long,
  val totalPages: Long,
  var records: List<R>? = null,
) {

  companion object {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T, B> from(page: T): Pagination<B> =
      (PageConverterHolder.converter as PageConverter<T>).from(page) as Pagination<B>
  }
}
