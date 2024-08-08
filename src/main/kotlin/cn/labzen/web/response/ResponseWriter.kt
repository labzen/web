package cn.labzen.web.response

import cn.labzen.tool.definition.Constants
import cn.labzen.spring.Springs
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.jvm.optionals.getOrNull

object ResponseWriter {

  private val objectMapper: ObjectMapper? by lazy {
    Springs.bean(ObjectMapper::class.java).getOrNull()
  }

  @JvmStatic
  fun sendMessage(message: Any, request: HttpServletRequest, response: HttpServletResponse) {
    val contentType = request.getHeader("Accept")

    response.status = HttpStatus.OK.value()
    response.contentType = contentType
    response.characterEncoding = Constants.DEFAULT_CHARSET_NAME
    try {
      objectMapper?.writeValue(response.writer, message)
      response.writer.flush()
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }
}