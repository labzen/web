package cn.labzen.web.response

import cn.labzen.tool.util.Strings
import cn.labzen.meta.Labzens
import cn.labzen.spring.Springs
import cn.labzen.web.REST_REQUEST_TIME
import cn.labzen.web.REST_REQUEST_TIME_MILLIS
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.response.result.Result
import cn.labzen.web.response.struct.Meta
import cn.labzen.web.response.struct.Response
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest

class LabzenResponseTransformer : ResponseTransformer {

  override fun transform(result: Any?, request: HttpServletRequest): Any {
    // 处理404
    val errorCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
    if (errorCode != null && result is Map<*, *>) {
      val code = result["status"].toString().toInt()
      return Response(code, result["error"].toString())
    }

    val requestTime = request.getAttribute(REST_REQUEST_TIME).toString()
    val requestMillis = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString().toLong()
    val executionTime = System.currentTimeMillis() - requestMillis
    return if (result is Result) {
      val code = result.code
      val message = result.message ?: "success"
      val data = result.value

      val meta = Meta(requestTime, executionTime, result.pagination)
      Response(code, message, meta, data)
    } else {
      Response(200, "success", Meta(requestTime, executionTime), result)
    }
  }

  companion object {
    internal fun createTransformer(): ResponseTransformer {
      val configuration = Labzens.configurationWith(WebConfiguration::class.java)
      val unifyRestResponseTransformer = configuration.unifyRestResponseTransformer()

      return if (Strings.isBlank(unifyRestResponseTransformer)) {
        LabzenResponseTransformer()
      } else {
        try {
          val customResponseTransformerClass: Class<*> = Class.forName(unifyRestResponseTransformer)
          if (!ResponseTransformer::class.java.isAssignableFrom(customResponseTransformerClass)) {
            // do nothing todo
            throw RuntimeException()
          }

          Springs.getOrCreate(customResponseTransformerClass) as ResponseTransformer
        } catch (e: Exception) {
          LabzenResponseTransformer()
        }
      }
    }
  }
}