package cn.labzen.web.spring.runtime

import cn.labzen.meta.Labzens
import cn.labzen.tool.util.DateTimes
import cn.labzen.tool.util.Strings
import cn.labzen.web.REST_REQUEST_TIME
import cn.labzen.web.REST_REQUEST_TIME_MILLIS
import cn.labzen.web.defination.APIVersionCarrier
import cn.labzen.web.meta.WebConfiguration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 拦截器，用于统计请求处理时长、以及其他与 Labzen Web 组件相关的功能处理
 */
class LabzenRestRequestHandlerInterceptor : HandlerInterceptor {

  private val forceRequestWithVersionHeader: Boolean by lazy {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    configuration.apiVersionCarrier() == APIVersionCarrier.HEADER && configuration.apiVersionHeaderAcceptForced()
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    // 强制请求的Header中带有Accept版本信息
    if (forceRequestWithVersionHeader) {
      val acceptHeader = request.getHeader("Accept")
      if (acceptHeader == null || !Strings.startsWith(acceptHeader, false, "application/vnd")) {
        response.sendError(
          HttpStatus.NOT_ACCEPTABLE.value(),
//          HttpServletResponse.SC_NOT_ACCEPTABLE,
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