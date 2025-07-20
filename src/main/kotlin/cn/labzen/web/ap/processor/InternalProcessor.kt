package cn.labzen.web.ap.processor

import cn.labzen.web.ap.internal.context.ControllerContext

/**
 *
 */
interface InternalProcessor {

  fun process(context: ControllerContext)

  /**
   * 处理器的优先级
   *
   * 该值最高优先级为最小的数字，最低优先级为最大数字；按照最高到最低的顺序调用处理器
   */
  fun priority(): Int

  companion object {
    internal const val PRIORITY_PREPARE = 1
    internal const val PRIORITY_READ_SOURCE = 2
    internal const val PRIORITY_READ_ANNOTATION = 3
    internal const val PRIORITY_EVALUATE_FIELDS = 4
    internal const val PRIORITY_EVALUATE_METHODS = 5
    internal const val PRIORITY_CREATIVE = 6
  }
}