package cn.labzen.web.response

import cn.labzen.web.response.format.ResponseFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Deprecated("")
class LabzenResponseFormatter : ResponseFormatter {

  override fun support(clazz: Class<*>, request: HttpServletRequest): Boolean {
    TODO("Not yet implemented")
  }

  override fun format(result: Any?, request: HttpServletRequest, response: HttpServletResponse): Any {
    TODO("Not yet implemented")
  }

//  override fun transform(result: Any?, request: HttpServletRequest): Any {
//    // 文件下载的Result由LabzenResourceMessageConverter负责处理，按理说也不会执行到这儿
//    if (result is DownloadableResult) {
//      return result
//    }
//
//    // 处理404
//    val errorCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
//    if (errorCode != null && result is Map<*, *>) {
//      val code = result["status"].toString().toInt()
//      return Response(code, result["error"].toString())
//    }
//
//    val requestTime = request.getAttribute(REST_REQUEST_TIME).toString()
//    val requestMillis = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString().toLong()
//    val executionTime = System.currentTimeMillis() - requestMillis
//    return if (result is Result) {
//      val code = result.code
//      val message = result.message ?: "success"
//      val data = result.value
//
//      val meta = Meta(requestTime, executionTime/*, result.pagination*/)
//      Response(code, message, meta, data)
//    } else {
//      Response(200, "success", Meta(requestTime, executionTime), result)
//    }
//  }
//
//  companion object {
//
//    // todo 重构这部分，写的不大好
//    internal fun createTransformer(): ResponseFormatter {
//      val configuration = Labzens.configurationWith(WebConfiguration::class.java)
//      val unifyRestResponseTransformer = configuration.responseFormatter()
//
//      return if (Strings.isBlank(unifyRestResponseTransformer)) {
//        LabzenResponseFormatter()
//      } else {
//        try {
//          val customResponseTransformerClass: Class<*> = Class.forName(unifyRestResponseTransformer)
//          if (!ResponseFormatter::class.java.isAssignableFrom(customResponseTransformerClass)) {
//            // do nothing to do
//            throw RuntimeException()
//          }
//
//          Springs.getOrCreate(customResponseTransformerClass) as ResponseFormatter
//        } catch (e: Exception) {
//          LabzenResponseFormatter()
//        }
//      }
//    }
//  }
}