package cn.labzen.web.spring

import cn.labzen.cells.core.utils.DateTimes
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LabzenRestRequestHandlerInterceptor : HandlerInterceptor {

//  private val requestTime: ThreadLocal<Long> = ThreadLocal()

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
//    requestTime.set(System.currentTimeMillis())
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
//    val requestTime = this.requestTime.get()
//    val executionTime = System.currentTimeMillis() - requestTime
//    request.setAttribute(REST_EXECUTION_TIME, executionTime)
//
//    this.requestTime.remove()
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?
  ) {
  }

}