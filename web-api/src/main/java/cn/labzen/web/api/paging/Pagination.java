package cn.labzen.web.api.paging;

import java.util.List;

public interface Pagination<R> {

  boolean pageable();

  int pageNumber();

  int pageSize();

  long totalRecords();

  long totalPages();

  List<R> records();

  Pagination<R> copyWithoutRecords();
}
