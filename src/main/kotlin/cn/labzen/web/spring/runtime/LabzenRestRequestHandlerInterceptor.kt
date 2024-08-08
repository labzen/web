package cn.labzen.web.spring.runtime

import cn.labzen.tool.util.DateTimes
import cn.labzen.tool.util.Strings
import cn.labzen.meta.Labzens
import cn.labzen.web.REST_REQUEST_TIME
import cn.labzen.web.REST_REQUEST_TIME_MILLIS
import cn.labzen.web.meta.WebConfiguration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LabzenRestRequestHandlerInterceptor : HandlerInterceptor {

  private val forceRequestWithVersionHeader: Boolean by lazy {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    configuration.controllerVersionHeaderForced()
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    // 强制请求的Header中带有Accept版本信息
    if (forceRequestWithVersionHeader) {
      val acceptHeader = request.getHeader("Accept")
      if (acceptHeader == null || !Strings.startsWith(acceptHeader, false, "application/vnd")) {
        response.sendError(
          HttpServletResponse.SC_NOT_ACCEPTABLE,
          "Accept header must be specified with a valid API version"
        )
        return false
      }
    }

    // 用于统计一个请求的用时
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