package cn.labzen.web.spring

import cn.labzen.cells.core.utils.Strings
import cn.labzen.meta.Labzens
import cn.labzen.meta.exception.LabzenRuntimeException
import cn.labzen.spring.Springs
import cn.labzen.web.exception.RequestException
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.response.LabzenResponseTransformer
import cn.labzen.web.response.ResponseTransformer
import cn.labzen.web.response.struct.Response
import cn.labzen.web.source.ControllerClassInitializer
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
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


@RestControllerAdvice
class LabzenRestResponseBody : ResponseBodyAdvice<Any>, InitializingBean {

  private var processAllRestResponse = true
  private val responseTransformer: ResponseTransformer = createTransformer()

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
    if (!selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
      return body
    }

    val httpRequest = if (request is ServletServerHttpRequest) {
      request.servletRequest
    } else return body

    return responseTransformer.transform(body, httpRequest)
  }

  @ExceptionHandler(LabzenRuntimeException::class)
  fun handleException(request: HttpServletRequest, e: LabzenRuntimeException): Any? {
//    val withSpringErrorPage = if (handlerMethod?.method?.isAnnotationPresent(Catching::class.java) == true) {
//      val catching = handlerMethod.method.getAnnotation(Catching::class.java)
//      // 只有在@Catching中声明的异常类，才按照Labzen的错误信息处理
//      catching?.exceptions?.find {
//        it.java.isAssignableFrom(e.javaClass)
//      } == null
//    } else {
//      // 没有注解@Catching，就按照Labzen的错误信息处理
//      false
//    }

//    if (withSpringErrorPage) {
//      val errorViewResolverOptional = Springs.bean(DefaultErrorViewResolver::class.java)
//      return if (errorViewResolverOptional.isPresent) {
//        val model = mutableMapOf<String, Any?>()
//        model["timestamp"] = Date()
//        model["error"] = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase
//        model["status"] = HttpStatus.INTERNAL_SERVER_ERROR.value()
//        model["message"] = e.message
//        model["path"] = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
//        ModelAndView("error", model);
//      } else null
//    }
    val le = if (e is RequestException) e else null
    // todo 在这里可以定制不同的异常类对应不同的http code
    val code = le?.code ?: HttpStatus.INTERNAL_SERVER_ERROR.value()

    return Response(code, e.message ?: "internal server error")
  }
}