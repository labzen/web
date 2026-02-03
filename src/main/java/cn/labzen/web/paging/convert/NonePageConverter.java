package cn.labzen.web.paging.convert;

import cn.labzen.web.paging.PageConverter;
import cn.labzen.web.paging.Pagination;
import cn.labzen.web.paging.internal.Paging;

public class NonePageConverter implements PageConverter<Paging> {

  @Override
  public Paging to(Paging paging) {
    return paging;
  }

  @Override
  public Pagination<?> from(Paging paging) {
    return new Pagination<>(!paging.unpaged(), paging.pageNumber(), paging.pageSize(), 0, 0, null);
  }
}
