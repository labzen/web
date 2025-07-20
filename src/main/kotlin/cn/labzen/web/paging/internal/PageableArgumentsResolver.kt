package cn.labzen.web.paging.internal

import cn.labzen.web.paging.Order
import cn.labzen.web.paging.Pageable
import org.springframework.web.context.request.NativeWebRequest

/**
 * 查询请求分页条件解析器，分页条件参数传递规则参考 [Pageable]
 */
object PageableArgumentsResolver {

  fun resolve(webRequest: NativeWebRequest): Paging? =
    webRequest.getParameter("paging")?.let {
      resolveFromCompact(it)
    } ?: resolveFromNormal(webRequest)

  private fun resolveFromCompact(raw: String): Paging? {
    val parts = raw.split(",")
    // 第一部分必须出现，且为正整数
    val pageNumber = parts.getOrNull(0)?.toIntOrNull() ?: return null

    val pageSize = parts.getOrNull(1)?.toIntOrNull()
    // 如果第二部分解析不到数据，则认为没有pageSize，orders从第二部分开始解析
    val ordersIndex = if (pageSize == null) 1 else 2

    val orderParts = parts.drop(ordersIndex)
    val orders = resolveOrders(orderParts)

    return Paging(false, pageNumber, pageSize ?: 0, orders)
  }

  private fun resolveFromNormal(webRequest: NativeWebRequest): Paging? {
    val pageNumber = webRequest.getParameter("page_number")?.toIntOrNull()
      ?: webRequest.getParameter("pn")?.toIntOrNull()
    val pageSize = webRequest.getParameter("page_size")?.toIntOrNull()
      ?: webRequest.getParameter("ps")?.toIntOrNull()
    val ordersRaw = webRequest.getParameter("orders") ?: webRequest.getParameter("od")
    val orders = ordersRaw?.split(',')?.filter { it.isNotBlank() }?.let { resolveOrders(it) } ?: emptyList()

    return if (pageNumber != null && pageSize != null) {
      Paging(false, pageNumber, pageSize, orders)
    } else null
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