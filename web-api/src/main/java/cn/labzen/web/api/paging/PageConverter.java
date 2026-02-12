package cn.labzen.web.api.paging;

public interface PageConverter<T> {

  T to(Pageable pageable);

  Pagination<?> from(T page);
}
