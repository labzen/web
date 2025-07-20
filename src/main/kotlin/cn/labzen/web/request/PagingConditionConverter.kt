package cn.labzen.web.request

/**
 * [Paging] 仅包含了请求的分页信息，但业务系统的ORM层等，可能会有其他的分页要求或特定的类。
 *
 * @param T 将 Request Page Condition 信息转换为业务系统查询所需的分页类型
 */
@Deprecated("重新实现")
interface PagingConditionConverter<T> {

  fun convert(condition: Paging): T
}