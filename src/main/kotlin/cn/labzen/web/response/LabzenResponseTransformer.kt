package cn.labzen.web.response

import cn.labzen.web.response.result.Result
import cn.labzen.web.response.struct.Meta
import cn.labzen.web.response.struct.Response
import cn.labzen.web.spring.REST_REQUEST_TIME
import cn.labzen.web.spring.REST_REQUEST_TIME_MILLIS
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
//    val executionTime = request.getAttribute(REST_EXECUTION_TIME)?.toString()?.toLong() ?: 0
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
}