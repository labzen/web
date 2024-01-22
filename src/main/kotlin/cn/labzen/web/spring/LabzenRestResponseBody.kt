package cn.labzen.web.spring

import cn.labzen.cells.core.utils.Strings
import cn.labzen.meta.Labzens
import cn.labzen.spring.Springs
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
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestControllerAdvice
class LabzenRestResponseBody : ResponseBodyAdvice<Any>, InitializingBean {

  private var processAllRestResponse = true
  private val responseTransformer: ResponseTransformer = createTransformer()
  private val isHeadVersionEnabled = Labzens.configurationWith(WebConfiguration::class.java).let {
    it.controllerVersionEnabled() && it.controllerVersionPlace() == RequestMappingVersionPlace.HEAD
  }

  private fun createTransformer(): ResponseTransformer {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    val unifyRestResponseTransformer = configuration.unifyRestResponseTransformer()

    return if (Strings.isBlank(unifyRestResponseTransformer)) {
      LabzenResponseTransformer()
    } else {
      try {
        val customResponseTransformerClass: Class<*> = Class.forName(unifyRestResponseTransformer)
        if (!ResponseTransformer::class.java.isAssignableFrom(customResponseTransformerClass)) {
          throw RuntimeException()
        }

        val foundTransformerBean: Optional<out Any> = Springs.bean(customResponseTransformerClass)
        if (foundTransformerBean.isPresent) {
          foundTransformerBean.get() as ResponseTransformer
        } else {
          customResponseTransformerClass.getConstructor().newInstance() as ResponseTransformer
        }
      } catch (e: Exception) {
        LabzenResponseTransformer()
      }
    }
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

    if (isHeadVersionEnabled) {
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
  fun handleException(request: HttpServletRequest, response: HttpServletResponse, e: RequestException): Any? {
    return Response(e.code, e.message ?: "internal server error")
  }
}