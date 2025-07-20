package cn.labzen.web.response.format

import cn.labzen.web.REST_REQUEST_TIME
import cn.labzen.web.REST_REQUEST_TIME_MILLIS
import cn.labzen.web.response.bean.Meta
import cn.labzen.web.response.bean.Response
import cn.labzen.web.response.bean.Result
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 格式化非 [Result] 类型的返回值
 */
class UnexpectedResponseFormatter : ResponseFormatter {

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean =
    true

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any {
    val requestTime = request.getAttribute(REST_REQUEST_TIME).toString()
    val requestMillis = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString().toLong()
    val executionTime = System.currentTimeMillis() - requestMillis

    return Response(200, "success", Meta(requestTime, executionTime), result)
  }

}