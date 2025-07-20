package cn.labzen.web.request

/**
 * 分页请求，
 */
@Deprecated("改用paging包下的 Pageable")
class Paging {

  /**
   * 当前页码，从0开始计数
   */
  var page: Int = 1

  /**
   * 默认每页20
   */
  var size: Int = defaultSize

  /**
   * 排序规则字符串：
   * 1. 字段名为数据库字段名，或映射java bean的属性名均可
   * 2. 默认升序，字段名后加英文叹号为降序
   * 3. 多个字段排序，字段间使用英文逗号分隔
   */
  var order: String = ""

  /**
   * 忽略分页规则
   *
   * 仅作为一个标识，提供给业务逻辑代码部分作为一个参考。不建议在查询大数据量表的地方，使用该标识。
   */
  var isUnpaged = false

  /**
   * @return 排序字段集合，Pair元素中，第一个是字段名，第二个为true - 升序，false - 降序
   */
  fun getOrders(): List<Order> {
    val parts = order.split(",")
    return parts.mapNotNull {
      if (it.isBlank()) null
      else {
        val isDesc = it.last() == '!'
        val column = if (isDesc) it.removeSuffix("!") else it
        Order(column, !isDesc)
      }
    }
  }

  /**
   * 转换为通过[PagingConditionConverter]接口实现转换的分页类
   */
  @Suppress("UNCHECKED_CAST")
  fun <T> convert(): T =
    converter?.convert(this) as T

  data class Order(val name: String, val isAsc: Boolean)

  companion object {

    internal var defaultSize = 20
    internal var converter: PagingConditionConverter<*>? = null
  }
}