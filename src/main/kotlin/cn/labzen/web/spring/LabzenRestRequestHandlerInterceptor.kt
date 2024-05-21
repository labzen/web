package cn.labzen.web.spring

import cn.labzen.cells.core.utils.DateTimes
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LabzenRestRequestHandlerInterceptor : HandlerInterceptor {

  /**
   * 用于统计一个请求的用时
   */
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    request.setAttribute(REST_REQUEST_TIME_MILLIS, System.currentTimeMillis())
    request.setAttribute(REST_REQUEST_TIME, DateTimes.formatNow())
    return true
  }

  override fun postHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    modelAndView: ModelAndView?
  ) {
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?
  ) {
  }

}