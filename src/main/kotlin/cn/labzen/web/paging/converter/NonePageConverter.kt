package cn.labzen.web.paging.converter

import cn.labzen.web.paging.PageConverter
import cn.labzen.web.paging.Pagination
import cn.labzen.web.paging.internal.Paging

class NonePageConverter : PageConverter<Paging> {

  override fun to(paging: Paging): Paging = paging

  override fun from(page: Paging): Pagination<*> =
    with(page) {
      Pagination<Any>(pageNumber(), pageSize(), 0, 0)
    }
}