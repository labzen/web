package cn.labzen.web.paging.internal

import cn.labzen.web.paging.Order
import cn.labzen.web.paging.Pageable
import org.springframework.web.context.request.NativeWebRequest

/**
 * 查询请求分页条件解析器，分页条件参数传递规则参考 [Pageable]
 */
object PageableResolver {

  fun resolve(webRequest: NativeWebRequest): Paging =
    webRequest.getParameter("paging")?.let {
      resolveFromCompact(it)
    } ?: resolveFromNormal(webRequest)

  private fun resolveFromCompact(raw: String): Paging {
    val parts = raw.split(",")
    // 第一部分必须出现，且为正整数，否则默认第一页
    val pageNumber = parts.getOrNull(0)?.toIntOrNull() ?: Pageable.DEFAULT_PAGE_NUMBER

    val pageSize = parts.getOrNull(1)?.toIntOrNull()
    // 如果第二部分解析不到数据，则认为没有pageSize，orders从第二部分开始解析
    val ordersIndex = if (pageSize == null) 1 else 2

    val orderParts = parts.drop(ordersIndex)
    val orders = resolveOrders(orderParts)

    // 使用 paging=1,20 这种形式的参数传递，不存在忽略分页需求的情况
    return Paging(false, pageNumber, pageSize ?: Pageable.DEFAULT_PAGE_SIZE, orders)
  }

  private fun resolveFromNormal(webRequest: NativeWebRequest): Paging {
    if (webRequest.getParameter("unpaged") != null) {
      return Paging.UNPAGED_PAGING
    }

    val pageNumber = webRequest.getParameter("page_number")?.toIntOrNull()
      ?: webRequest.getParameter("pn")?.toIntOrNull() ?: Pageable.DEFAULT_PAGE_NUMBER
    val pageSize = webRequest.getParameter("page_size")?.toIntOrNull()
      ?: webRequest.getParameter("ps")?.toIntOrNull() ?: Pageable.DEFAULT_PAGE_SIZE
    val ordersRaw = webRequest.getParameter("orders") ?: webRequest.getParameter("od")
    val orders = ordersRaw?.split(',')?.filter { it.isNotBlank() }?.let { resolveOrders(it) } ?: emptyList()

    return Paging(false, pageNumber, pageSize, orders)
  }

  private fun resolveOrders(orderParts: List<String>): List<Order> =
    orderParts.mapNotNull { chip ->
      val trimmed = chip.trim()
      if (trimmed.isEmpty()) return@mapNotNull null

      val name = trimmed.takeWhile { it != '!' && it != '+' && it != '-' }

      val asc = !trimmed.contains('!')
      val nulls: String? = if (trimmed.endsWith('+')) "first"
      else if (trimmed.endsWith('-')) "last"
      else null

      Order(name, asc, nulls)
    }
}