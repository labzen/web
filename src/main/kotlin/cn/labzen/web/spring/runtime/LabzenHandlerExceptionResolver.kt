package cn.labzen.web.spring.runtime

import cn.labzen.logger.Loggers
import cn.labzen.web.EXCEPTION_WAS_LOGGED_DURING_REQUEST
import cn.labzen.web.response.LabzenResponseTransformer
import cn.labzen.web.response.ResponseTransformer
import cn.labzen.web.response.result.Result
import cn.labzen.web.response.struct.Response
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.GenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.NoHandlerFoundException
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Order(value = Ordered.HIGHEST_PRECEDENCE)
class LabzenHandlerExceptionResolver : HandlerExceptionResolver {

  @Resource
  lateinit var converters: List<HttpMessageConverter<Any>>
  private val responseTransformer: ResponseTransformer = LabzenResponseTransformer.createTransformer()

  override fun resolveException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any?,
    ex: Exception
  ): ModelAndView? {
    val attribute = request.getAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST)
    if (attribute == null) {
      val logger = Loggers.getLogger(ex.stackTrace[0].className)
      logger.error(ex)
      request.setAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST, true)
    }

    return when (ex) {
      is BindException -> handleBindException(request, response, ex)
      is NoHandlerFoundException -> handleNoHandlerFoundException(request, response)
      is HttpRequestMethodNotSupportedException -> handleRequestMethodNotSupportedException(request, response, ex)
      is HttpMediaTypeNotSupportedException -> handleMediaTypeNotSupportedException(request, response, ex)
      is MissingPathVariableException -> handleMissingPathVariableException(request, response, ex)
      is MissingServletRequestParameterException -> handleMissingServletRequestParameterException(request, response, ex)
      is ServletRequestBindingException -> handleServletRequestBindingException(request, response, ex)
      is ConversionNotSupportedException -> handleConversionNotSupportedException(request, response, ex)
      is TypeMismatchException -> handleTypeMismatchException(request, response, ex)
      else -> null
    }
  }

  private fun handleBindException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: BindException
  ): ModelAndView {
    val allErrors = exception.bindingResult.allErrors.associate {
      if (it is FieldError)
        Pair(it.field, it.defaultMessage)
      else
        Pair(it.objectName, it.defaultMessage)
    }
    val data = mapOf(Pair("validator", allErrors))
    responseWithData(HttpStatus.BAD_REQUEST, data, request, response)
    return ModelAndView()
  }

  private fun handleNoHandlerFoundException(
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ModelAndView {
    responseNoData(HttpStatus.NOT_FOUND, request, response)
    return ModelAndView()
  }

  private fun handleRequestMethodNotSupportedException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: HttpRequestMethodNotSupportedException
  ): ModelAndView {
    val supportedMethods = exception.supportedMethods?.toList()
    supportedMethods?.let {
      responseWithData(HttpStatus.METHOD_NOT_ALLOWED, supportedMethods, request, response)
    } ?: responseNoData(HttpStatus.METHOD_NOT_ALLOWED, request, response)
    return ModelAndView()
  }

  private fun handleMediaTypeNotSupportedException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: HttpMediaTypeNotSupportedException
  ): ModelAndView {
    responseWithMessage(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.message, request, response)
    return ModelAndView()
  }

  private fun handleMissingPathVariableException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: MissingPathVariableException
  ): ModelAndView {
    responseWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, exception.message, request, response)
    return ModelAndView()
  }

  private fun handleMissingServletRequestParameterException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: MissingServletRequestParameterException
  ): ModelAndView {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.message, request, response)
    return ModelAndView()
  }

  private fun handleServletRequestBindingException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: ServletRequestBindingException
  ): ModelAndView {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.message, request, response)
    return ModelAndView()
  }

  private fun handleConversionNotSupportedException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: ConversionNotSupportedException
  ): ModelAndView {
    responseWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, exception.message, request, response)
    return ModelAndView()
  }

  private fun handleTypeMismatchException(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: TypeMismatchException
  ): ModelAndView {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.message, request, response)
    return ModelAndView()
  }

  private fun responseWithMessage(
    status: HttpStatus,
    message: String?,
    request: HttpServletRequest,
    response: HttpServletResponse
  ) {
    val result = Result.withStatus(status, null, message)
    val resp = responseTransformer.transform(result, request)
    out(resp, request, response)
  }

  private fun responseNoData(
    status: HttpStatus,
    request: HttpServletRequest,
    response: HttpServletResponse
  ) {
    val result = Result.withStatus(status)
    val resp = responseTransformer.transform(result, request)
    out(resp, request, response)
  }

  private fun responseWithData(
    status: HttpStatus,
    data: Any,
    request: HttpServletRequest,
    response: HttpServletResponse
  ) {
    val result = Result.withStatus(status, data)
    val resp = responseTransformer.transform(result, request)
    out(resp, request, response)
  }

  private fun out(
    data: Any,
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val mediaType = MediaType.parseMediaType(request.getHeader("Accept"))

    for (converter in converters) {
      val genericConverter: GenericHttpMessageConverter<Any>? =
        if (converter is GenericHttpMessageConverter) converter else null
      val outMessage = ServletServerHttpResponse(response)

      if (genericConverter != null && genericConverter.canWrite(RESPONSE_TYPE, RESPONSE_TYPE, mediaType)) {
        genericConverter.write(data, RESPONSE_TYPE, mediaType, outMessage)
      } else if (converter.canWrite(RESPONSE_TYPE, mediaType)) {
        converter.write(data, mediaType, outMessage)
      }
    }
  }

  companion object {
    private val RESPONSE_TYPE = Response::class.java
  }
}