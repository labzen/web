package cn.labzen.web.spring

import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

class LabzenWebConfigurer : WebMvcConfigurer {

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(LabzenRestRequestHandlerInterceptor())
  }
}