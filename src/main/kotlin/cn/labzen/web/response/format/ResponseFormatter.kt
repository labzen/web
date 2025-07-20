package cn.labzen.web.response.format

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 响应内容格式化接口，处理Result结果中的数据
 */
interface ResponseFormatter {

  fun support(clazz: Class<*>, request: HttpServletRequest): Boolean

  fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any
}