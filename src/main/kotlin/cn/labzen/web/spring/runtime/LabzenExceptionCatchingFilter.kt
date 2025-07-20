package cn.labzen.web.spring.runtime

import cn.labzen.logger.Loggers
import cn.labzen.meta.exception.LabzenException
import cn.labzen.meta.exception.LabzenRuntimeException
import cn.labzen.spring.Springs
import cn.labzen.tool.definition.Constants
import cn.labzen.web.EXCEPTION_WAS_LOGGED_DURING_REQUEST
import cn.labzen.web.exception.RequestException
import cn.labzen.web.response.bean.Response
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 异常捕捉过滤器，防止有异常没有经过 [HandlerExceptionResolver] 处理，而被直接抛出到前端
 */
class LabzenExceptionCatchingFilter : OncePerRequestFilter() {

  private val objectMapper: ObjectMapper by lazy {
    Springs.bean(ObjectMapper::class.java).orElseGet { ObjectMapper() }
  }

  /**
   * #第3级异常拦截处理：最后的抛出异常捕捉拦截保障，最后的一次兜底，保证拦截所有的异常，然后对异常做统一结构格式化输出
   */
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    try {
      filterChain.doFilter(request, response)
    } catch (e: Exception) {
      logging(request, e)

      output(request, response, e)
    }
  }

  private fun logging(request: HttpServletRequest, e: Exception) {
    // EXCEPTION_WAS_LOGGED_DURING_REQUEST 防止重复打印
    val attribute = request.getAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST)
    if (attribute == null) {
      // 如果是 RequestException 异常，则看其配置是否需要打印。否则均打印日志
      if (e is RequestException && e.logging || e !is RequestException) {
        val logger = Loggers.getLogger(e.stackTrace[0].className)
        logger.error(e)
        request.setAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST, true)
      }
    }
  }

  private fun output(request: HttpServletRequest, response: HttpServletResponse, e: Exception) {
    when (e) {
      is RequestException -> {
        Loggers.getLogger(LabzenExceptionCatchingFilter::class.java)
          .warn("RequestException 应该在 LabzenRestResponseBodyAdvice 的 handleLabzenRequestException() 方法中被转化，而不是被抛出到最外层")
        val data = Response(e.code, e.message ?: HttpStatus.BAD_REQUEST.reasonPhrase)
        sendMessage(data, request, response)
      }

      is LabzenRuntimeException, is LabzenException -> Response(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        e.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
      ).let { sendMessage(it, request, response) }

      else -> {
        val throwable = findRootCause(e)
        Response(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          throwable.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
        ).let { sendMessage(it, request, response) }
      }
    }
  }

  private fun findRootCause(exception: Throwable): Throwable {
    return if (exception.cause == null) exception
    else if (exception == exception.cause) exception
    else findRootCause(exception.cause!!)
  }

  private fun sendMessage(message: Any, request: HttpServletRequest, response: HttpServletResponse) {
    val contentType = request.getHeader("Accept")

    response.status = HttpStatus.OK.value()
    response.contentType = contentType
    response.characterEncoding = Constants.DEFAULT_CHARSET_NAME
    try {
      objectMapper.writeValue(response.writer, message)
      response.writer.flush()
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }
}