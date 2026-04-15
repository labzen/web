package cn.labzen.web.api.paging;

import java.util.List;

/**
 * 分页结果接口。
 * <p>
 * 定义了分页查询结果的通用结构，包含分页信息和数据记录。
 *
 * @param <R> 数据记录类型
 */
public interface Pagination<R> {

  /**
   * 是否支持分页
   *
   * @return true 表示支持分页，false 表示查询全部数据
   */
  boolean pageable();

  /**
   * 获取当前页码
   *
   * @return 当前页码（从 1 开始）
   */
  int pageNumber();

  /**
   * 获取每页数据量
   *
   * @return 每页数据条数
   */
  int pageSize();

  /**
   * 获取总记录数
   *
   * @return 总记录数
   */
  long totalRecords();

  /**
   * 获取总页数
   *
   * @return 总页数
   */
  long totalPages();

  /**
   * 获取当前页的数据记录
   *
   * @return 数据记录列表
   */
  List<R> records();

  /**
   * 复制分页信息（不包含数据记录）
   *
   * @return 仅包含分页信息的副本
   */
  Pagination<R> copyWithoutRecords();
}
