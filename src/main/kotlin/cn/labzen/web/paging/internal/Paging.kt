package cn.labzen.web.paging.internal

import cn.labzen.meta.Labzens
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.paging.Order
import cn.labzen.web.paging.Pageable
import cn.labzen.web.paging.Pageable.Companion.DEFAULT_PAGE_NUMBER

/**
 * 存储分页条件数据，用于[Pageable]数据代理提供方
 */
data class Paging(
  private var unpaged: Boolean,
  private var pageNumber: Int,
  private var pageSize: Int,
  private var orders: List<Order>,
) : Pageable {

  init {
    if (pageNumber <= 0) {
      pageNumber = DEFAULT_PAGE_NUMBER
    }
    if (pageSize <= 0) {
      pageSize = defaultPageSize
    }
  }

  override fun isUnpaged() = unpaged

  override fun pageNumber() = pageNumber

  override fun pageSize() = pageSize

  override fun orders() = orders

  override fun <T> convertTo(): T? {
    throw UnsupportedOperationException("not invoke this convert method")
  }

  companion object {
    private val defaultPageSize by lazy { Labzens.configurationWith(WebConfiguration::class.java).pageSize() }
  }
}