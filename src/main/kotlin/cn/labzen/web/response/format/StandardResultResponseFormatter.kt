package cn.labzen.web.response.format

import cn.labzen.web.REST_REQUEST_TIME
import cn.labzen.web.REST_REQUEST_TIME_MILLIS
import cn.labzen.web.paging.Pagination
import cn.labzen.web.response.bean.Meta
import cn.labzen.web.response.bean.Response
import cn.labzen.web.response.bean.Result
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 处理 [Result] 中的返回值格式化，通用的格式化器，在 [CompositeResponseFormatter] 中的执行顺序最晚，如果其他的格式化器不处理返回值，则在这里格式化，算是最后托底的
 */
class StandardResultResponseFormatter : ResponseFormatter {

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean =
    clazz == Result::class.java

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any {
    val requestTime = request.getAttribute(REST_REQUEST_TIME).toString()
    val requestMillis = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString().toLong()
    val executionTime = System.currentTimeMillis() - requestMillis

    val data = result as Result

    val code = data.code
    val message = data.message ?: "success"
    val value = data.value

    return if (value is Pagination<*>) {
      val records = value.records
      value.records = null
      val meta = Meta(requestTime, executionTime, value)
      Response(code, message, meta, records)
    } else {
      val meta = Meta(requestTime, executionTime)
      Response(code, message, meta, value)
    }
  }
}