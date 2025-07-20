package cn.labzen.web.response.format

import cn.labzen.web.response.bean.Response
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 格式化不正常的 Http Status 结果，如404
 */
class AbnormalStatusResponseFormatter : ResponseFormatter {

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean =
    clazz == Map::class.java && request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any {
    val data = result as Map<*, *>
    val code = data["status"].toString().toInt()
    val message = data["error"].toString()
    return Response(code, message)
  }
}