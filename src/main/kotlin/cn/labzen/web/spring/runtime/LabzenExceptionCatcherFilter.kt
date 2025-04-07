package cn.labzen.web.spring.runtime

import cn.labzen.logger.Loggers
import cn.labzen.meta.exception.LabzenException
import cn.labzen.meta.exception.LabzenRuntimeException
import cn.labzen.web.EXCEPTION_WAS_LOGGED_DURING_REQUEST
import cn.labzen.web.exception.RequestException
import cn.labzen.web.response.ResponseWriter
import cn.labzen.web.response.struct.Response
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class LabzenExceptionCatcherFilter : OncePerRequestFilter() {

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    try {
      filterChain.doFilter(request, response)
    } catch (e: Exception) {
      val attribute = request.getAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST)
      if (attribute == null) {
        if (e is RequestException && e.logging || e !is RequestException) {
          val logger = Loggers.getLogger(e.stackTrace[0].className)
          logger.error(e)
          request.setAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST, true)
        }
      }

      when (e) {
        is RequestException -> Response(
          e.code,
          e.message ?: HttpStatus.BAD_REQUEST.reasonPhrase,
          null,
          null
        ).let { ResponseWriter.sendMessage(it, request, response) }

        is LabzenRuntimeException, is LabzenException -> Response(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          e.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
          null,
          null
        ).let { ResponseWriter.sendMessage(it, request, response) }

        else -> {
          val throwable = findRootCause(e)
          Response(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            throwable.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            null,
            null
          ).let { ResponseWriter.sendMessage(it, request, response) }
        }
      }
    }
  }

  private fun findRootCause(exception: Throwable): Throwable {
    return if (exception.cause == null) exception
    else if (exception == exception.cause) exception
    else findRootCause(exception.cause!!)
  }

}