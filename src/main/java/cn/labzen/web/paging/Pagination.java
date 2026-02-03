package cn.labzen.web.paging;

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
public record Pagination<R>(
  boolean pageable,
  int pageNumber,
  int pageSize,
  long totalRecords,
  long totalPages,
  List<R> records
) {

  public static <T> Pagination<T> justRecords(List<T> records) {
    return new Pagination<>(false, 0, 0, 0, 0, records);
  }

  @SuppressWarnings("unchecked")
  public static <T, B> Pagination<B> from(T page) {
    return (Pagination<B>) ((PageConverter<T>) PageConverterHolder.getConverter()).from(page);
  }

  public Pagination<R> copyWithoutRecords() {
    return new Pagination<>(this.pageable, this.pageNumber, this.pageSize, this.totalRecords, this.totalPages, null);
  }
}
