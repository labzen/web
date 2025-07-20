package cn.labzen.web.paging

/**
 * 分页的 order by 条件
 */
data class Order(
  /**
   * 排序依据 表字段/映射类属性
   */
  val name: String,
  /**
   * asc or desc
   */
  val asc: Boolean = true,
  /**
   * 为兼容支持 NULLS FIRST|LAST 语法的数据库，只接受 "first" 或 "last" 不区分大小写
   */
  val nulls: String? = null,
)
