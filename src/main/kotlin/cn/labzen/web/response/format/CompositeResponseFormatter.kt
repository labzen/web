package cn.labzen.web.response.format

import cn.labzen.web.response.bean.Response
import org.springframework.aop.support.AopUtils
import org.springframework.http.HttpStatus
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 复合响应内容转换器
 */
class CompositeResponseFormatter : ResponseFormatter {

  private val formatters: List<ResponseFormatter>

  init {
    val responseAgainPF = ResponseAgainResponseFormatter()
    val abnormalStatusPF = AbnormalStatusResponseFormatter()
    val standardResultPF = StandardResultResponseFormatter()
    val unexpectedPF = UnexpectedResponseFormatter()

    val allFormatters = mutableListOf(responseAgainPF, abnormalStatusPF, unexpectedPF)
    val loadedFormatters = ServiceLoader.load(ResponseFormatter::class.java, javaClass.classLoader)
    allFormatters.addAll(loadedFormatters)
    allFormatters.add(standardResultPF)

    formatters = allFormatters.toList()
  }

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean =
    true

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any {
    result ?: return Response(HttpStatus.NO_CONTENT.value(), HttpStatus.NO_CONTENT.reasonPhrase)

    val targetClass = AopUtils.getTargetClass(result)
    for (formatter in formatters) {
      if (formatter.support(targetClass, request)) {
        return formatter.format(result, request, response)
      }
    }

    return Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "不阔能，绝对不阔能")
  }
}