package cn.labzen.web.api.paging;

/**
 * 分页转换器接口。
 * <p>
 * 用于在 {@link Pageable} 分页请求参数和特定分页实现之间进行转换。
 * 可通过 labzen.yml 配置项 {@code page.converter-pageable} 指定具体实现类。
 *
 * @param <T> 特定分页类型
 */
public interface PageConverter<T> {

  /**
   * 将分页请求参数转换为特定分页类型
   *
   * @param pageable 分页请求参数
   * @return 特定分页类型的实例
   */
  T to(Pageable pageable);

  /**
   * 将特定分页类型转换为通用分页结果
   *
   * @param page 特定分页类型实例
   * @return 通用分页结果
   */
  Pagination<?> from(T page);
}
