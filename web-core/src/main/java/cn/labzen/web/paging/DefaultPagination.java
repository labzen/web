package cn.labzen.web.paging;

import cn.labzen.web.api.paging.PageConverter;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.api.paging.Pagination;
import cn.labzen.web.paging.convert.PageConverterHolder;

import java.util.List;

/**
 * 分页结果信息
 *
 * @param pageable     开启分页功能
 * @param pageNumber   当前页
 * @param pageSize     每页记录数
 * @param totalRecords 记录总数
 * @param totalPages   总页数
 * @param records      查询记录结果
 */
public record DefaultPagination<R>(
  boolean pageable,
  int pageNumber,
  int pageSize,
  long totalRecords,
  long totalPages,
  List<R> records
) implements Pagination<R> {

  public static <T> DefaultPagination<T> justRecords(List<T> records) {
    return new DefaultPagination<>(false, 0, 0, 0, 0, records);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Pageable, B> DefaultPagination<B> from(T page) {
    return (DefaultPagination<B>) ((PageConverter<T>) PageConverterHolder.getConverter()).from(page);
  }

  public DefaultPagination<R> copyWithoutRecords() {
    return new DefaultPagination<>(this.pageable, this.pageNumber, this.pageSize, this.totalRecords, this.totalPages, null);
  }
}
