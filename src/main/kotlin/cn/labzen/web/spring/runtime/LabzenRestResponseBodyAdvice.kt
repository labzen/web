package cn.labzen.web.spring.runtime

import cn.labzen.meta.Labzens
import cn.labzen.web.exception.RequestException
import cn.labzen.web.meta.RequestMappingVersionPlace
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.response.LabzenResponseTransformer
import cn.labzen.web.response.ResponseTransformer
import cn.labzen.web.response.struct.Response
import cn.labzen.web.source.ControllerClassInitializer
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 转换 Http Response 结构
 */
@RestControllerAdvice
class LabzenRestResponseBodyAdvice : ResponseBodyAdvice<Any>, InitializingBean {

  private var processAllRestResponse = true
  private val responseTransformer: ResponseTransformer = LabzenResponseTransformer.createTransformer()
  private val isHeaderVersionEnabled = Labzens.configurationWith(WebConfiguration::class.java).let {
    it.controllerVersionEnabled() && it.controllerVersionPlace() == RequestMappingVersionPlace.HEADER
  }

  override fun afterPropertiesSet() {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    processAllRestResponse = configuration.unifyAllRestResponse()
  }

  override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
    return if (!processAllRestResponse) {
      // 如果是生成的动态Controller类中返回的response，就统一格式
      ControllerClassInitializer.controllerClasses.contains(returnType.declaringClass)
    } else {
      true
    }
  }

  override fun beforeBodyWrite(
    body: Any?,
    returnType: MethodParameter,
    selectedContentType: MediaType,
    selectedConverterType: Class<out HttpMessageConverter<*>>,
    request: ServerHttpRequest,
    response: ServerHttpResponse
  ): Any? {
    if (body is Response) {
      return body
    }

    if (isHeaderVersionEnabled) {
      if (!"json".equals(selectedContentType.subtype, true) && !"application".equals(selectedContentType.type, true)) {
        return body
      }
    } else if (!selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
      return body
    }

    val httpRequest = if (request is ServletServerHttpRequest) {
      request.servletRequest
    } else return body

    return responseTransformer.transform(body, httpRequest)
  }

  @ExceptionHandler(RequestException::class)
  fun handleLabzenException(request: HttpServletRequest, response: HttpServletResponse, e: RequestException): Any {
    return Response(e.code, e.message ?: "internal server error")
  }
}