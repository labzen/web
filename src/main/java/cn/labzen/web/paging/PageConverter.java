package cn.labzen.web.paging;

import cn.labzen.web.paging.internal.Paging;

public interface PageConverter<T> {

  T to(Paging paging);

  Pagination<?> from(T page);
}
