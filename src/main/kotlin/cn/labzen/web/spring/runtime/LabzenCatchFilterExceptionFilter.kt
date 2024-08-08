package cn.labzen.web.spring.runtime

import cn.labzen.meta.exception.LabzenException
import cn.labzen.meta.exception.LabzenRuntimeException
import cn.labzen.web.exception.RequestException
import cn.labzen.web.response.ResponseWriter
import cn.labzen.web.response.struct.Response
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LabzenCatchFilterExceptionFilter : OncePerRequestFilter() {

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    try {
      filterChain.doFilter(request, response)
    } catch (e: Exception) {
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

        else -> Response(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          e.message ?: HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
          null,
          null
        ).let { ResponseWriter.sendMessage(it, request, response) }
      }
    }
  }

}