package cn.labzen.web.response

/**
 * [Pagination] 包含查询后的分页基本信息，但业务系统查询回来的结构，并不一定使用该类，为方便统一 Http Response 的页面信息返回，提供了这个转换器。
 *
 * @param T 将业务系统查询后得到的分页信息类（T）转换为 Response Page
 */
interface PaginationConverter <T> {

  fun convert(page: T): Pagination
}