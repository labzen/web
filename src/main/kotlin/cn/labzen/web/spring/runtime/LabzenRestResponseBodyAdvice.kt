package cn.labzen.web.spring.runtime

import cn.labzen.meta.Labzens
import cn.labzen.web.exception.RequestException
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.response.bean.Response
import cn.labzen.web.response.bean.Result
import cn.labzen.web.response.format.CompositeResponseFormatter
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 对 Controller 返回的响应结果进行增强处理（转换 Http Response 结构）
 */
@RestControllerAdvice
class LabzenRestResponseBodyAdvice : ResponseBodyAdvice<Any>, InitializingBean {

  private val responseFormatter = CompositeResponseFormatter()
  private var processAllRestResponse = true

  override fun afterPropertiesSet() {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    processAllRestResponse = configuration.responseFormattingForcedAll()
  }

  override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean =
    if (processAllRestResponse) true
    else returnType.parameterType == Result::class.java

  override fun beforeBodyWrite(
    body: Any?,
    returnType: MethodParameter,
    selectedContentType: MediaType,
    selectedConverterType: Class<out HttpMessageConverter<*>>,
    request: ServerHttpRequest,
    response: ServerHttpResponse
  ): Any? {
    val httpRequest = if (request is ServletServerHttpRequest) {
      request.servletRequest
    } else return body
    val httpResponse = if (response is ServletServerHttpResponse) {
      response.servletResponse
    } else return body

    return responseFormatter.format(body, httpRequest, httpResponse)
  }

  /**
   * #第1级异常拦截处理：在这里处理业务相关异常，推荐统一封装为 [RequestException] !!
   */
  @ExceptionHandler(RequestException::class)
  fun handleLabzenRequestException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    e: RequestException
  ): Any {
    return Response(e.code, e.message ?: "internal server error")
  }
}