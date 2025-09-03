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
    // 第1 处理已经是 Response 结构化的情况，快速返回已经是 Response 结构的情况
    val responseAgainPF = ResponseAgainResponseFormatter()
    // 第2 格式化不正常的 Http Status 结果，如404
    val abnormalStatusPF = AbnormalStatusResponseFormatter()
    // 倒2 处理 Result 中的返回值格式化，标准的返回结构
    val standardResultPF = StandardResultResponseFormatter()
    // 倒1 处理前面所有格式化器都未考虑到的请
    val unexpectedPF = UnexpectedResponseFormatter()

    val allFormatters = mutableListOf(responseAgainPF, abnormalStatusPF)
    val loadedFormatters = ServiceLoader.load(ResponseFormatter::class.java, javaClass.classLoader)
    allFormatters.addAll(loadedFormatters)
    allFormatters.add(standardResultPF)
    allFormatters.add(unexpectedPF)

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