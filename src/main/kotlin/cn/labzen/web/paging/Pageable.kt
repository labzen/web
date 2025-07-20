package cn.labzen.web.paging

import cn.labzen.web.spring.runtime.PageableArgumentResolver
import java.io.Serializable
import java.lang.reflect.Type

/**
 * 标识可分页的资源，存储分页请求数据，由 [PageableArgumentResolver] 解析分页请求参数，并代理资源Bean来获取分页相关数据。
 *
 * ***不建议数据库表映射 Java Bean 实现本接口***，推荐定义数据传输对象DTO，作为前端请求参数与后端数据库表存储结构之间的数据
 *
 * 分页的三个主要条件：
 * 1. page number: 查询当前页码，必须是正整数 >=1，传递参数名为：`page_number` 或 `pn`；使用压缩方式时，该值必须最先出现，且不能缺失
 * 2. page size: 查询的每页数据量上限，必须是正整数 >=1，传递参数名为：`page_size` 或 `ps`；使用压缩方式时，该值必须排在page number之后，并与之以英文逗号分割
 * 3. orders: 查询的数据排序规则，传递参数名为：`orders` 或 `od`；使用压缩方式时，该值必须出现在最后，并与之前的值以英文逗号分割。orders的值规则如下：
 *     - 可同时指定多个order规则，order值语义：`(column_name | field_name)[!][+|-]`
 *     - 多个order之间使用英文逗号分隔
 *     - 可指定 column_name 或 field_name，由后端业务逻辑处理为数据库ORM层可处理的表字段名
 *     - 在 column_name | field_name 后紧跟英文叹号(!)，标识按照该字段做倒序排序，如无叹号，则为升序
 *     - 如在 column_name | field_name 后出现英文加或减号(+|-)，则标识按照该字段排序时指定 nulls first | last （需数据库支持）
 *
 * 要做分页查询的请求中，可通过两种方式传递分页相关参数：
 * #### 1. 常规方式：按照本接口方法所对应的参数名（转换为snake_case）传递，例如：
 * ```javascript
 * // 参数名严格映射到方法名
 * const url = '/resources?page_number=8&page_size=100';
 * /*
 *  * 参数名使用约定的缩写传递（需注意不要与业务参数名冲突）
 *  * page_number --> pn
 *  * page_size   --> ps
 *  * orders      --> od
 *  */
 * const url = '/resources?pn=8&ps=100';
 * ```
 * #### 2. 压缩方式：通过一个参数名传递所有分页相关数据，例如：
 * ```javascript
 * /*
 *  * 分页数据压缩排列顺序为 page_number, page_size, orders。使用英文逗号分割，page_number不能缺失，另两个部分可缺失
 *  */
 * // 查询第1页，每页20条
 * const url = '/resources?paging=1,20';
 * // 查询第4页，每页条数使用默认值
 * const url = '/resources?paging=4';
 * // 查询第1页，按照age字段倒序排序
 * const url = '/resources?paging=1,age!';
 * // 查询第4页，每页10条，按照scoreTotal属性倒序排序，nulls first
 * const url = '/resources?paging=4,10,scoreTotal!+';
 * ```
 */
interface Pageable : Serializable {

  /**
   * 忽略分页规则（即并未传递任何的分页条件）
   *
   * 仅作为一个标识，提供给业务逻辑代码部分作为一个参考。不建议在查询大数据量表的地方，使用该标识
   */
  fun isUnpaged(): Boolean = true

  /**
   * 当前页码，从1开始
   */
  fun pageNumber(): Int = DEFAULT_PAGE_NUMBER

  /**
   * 每页的数据查询数量上限，默认20
   */
  fun pageSize(): Int = DEFAULT_PAGE_SIZE

  /**
   * 获取所有的分页排序条件
   */
  fun orders(): List<Order> = emptyList()

  /**
   * 将分页数据转换为特定的分页Bean
   *
   * 具体的转换，需实现 [PageConverter] 接口，通过 `labzen.yml` 配置项 `page.converter-pageable` 指定具体类 FQCN
   *
   * @param clazz 特定Bean类型
   */
  fun <T> to(clazz: Class<T>): T? = null

  fun <T> to(type: Type): T? = null

  // todo 看情况，尽量保留这个方法，而不是 to(clazz)
  fun <T> convertTo(): T? = null

  companion object {
    internal const val DEFAULT_PAGE_NUMBER = 1
    internal const val DEFAULT_PAGE_SIZE = 20
  }
}