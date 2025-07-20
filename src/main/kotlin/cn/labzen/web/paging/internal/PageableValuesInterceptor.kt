package cn.labzen.web.paging.internal

import cn.labzen.web.paging.converter.PageConverterHolder
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import java.lang.reflect.Method

class PageableValuesInterceptor(private val paging: Paging) {

  @RuntimeType
  fun intercept(@Origin method: Method): Any =
    when (method.name) {
      "isUnpaged" -> paging.isUnpaged()
      "pageNumber" -> paging.pageNumber()
      "pageSize" -> paging.pageSize()
      "orders" -> paging.orders()
      "convertTo" -> PageConverterHolder.converter.to(paging)!!
      else -> throw IllegalStateException()
    }
}