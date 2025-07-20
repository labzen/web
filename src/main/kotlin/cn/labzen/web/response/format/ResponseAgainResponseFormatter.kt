package cn.labzen.web.response.format

import cn.labzen.web.response.bean.Response
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 处理已经是 [Response] 结构化的情况
 */
class ResponseAgainResponseFormatter : ResponseFormatter {

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean =
    clazz == Response::class.java

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any =
    result!!
}