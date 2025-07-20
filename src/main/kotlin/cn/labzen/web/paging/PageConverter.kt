package cn.labzen.web.paging

import cn.labzen.web.paging.internal.Paging

/**
 * todo 将 pageable 和 pagination 两个类的转换器，合并为一个
 */
interface PageConverter<T> {

  fun to(paging: Paging): T

  fun from(page: T): Pagination<*>
}