package cn.labzen.web.paging.convert;

import cn.labzen.web.api.paging.PageConverter;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.paging.DefaultPagination;
import cn.labzen.web.paging.internal.Paging;

public class NonePageConverter implements PageConverter<Paging> {

  @Override
  public Paging to(Pageable pageable) {
    if (pageable instanceof Paging paging) {
      return paging;
    }
    return new Paging(pageable.unpaged(), pageable.pageNumber(), pageable.pageSize(), pageable.orders());
  }

  @Override
  public DefaultPagination<?> from(Paging paging) {
    return new DefaultPagination<>(!paging.unpaged(), paging.pageNumber(), paging.pageSize(), 0, 0, null);
  }
}
