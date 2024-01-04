package cn.labzen.web.response

import javax.servlet.http.HttpServletRequest

interface ResponseTransformer {

  fun transform(result: Any?, request: HttpServletRequest): Any
}