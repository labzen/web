package cn.labzen.web.spring

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.meta.Labzens
import cn.labzen.web.meta.WebConfiguration
import com.google.common.base.Strings
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.UrlPathHelper

class LabzenWebConfigurer : WebMvcConfigurer {

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(LabzenRestRequestHandlerInterceptor())
  }

  override fun configurePathMatch(configurer: PathMatchConfigurer) {
    configurer.setUrlPathHelper(UrlPathHelper())

    val configuration = Labzens.configurationWith(WebConfiguration::class.java)

    val apiPathPrefix = configuration.apiPathPrefix()
    if (!Strings.isNullOrEmpty(apiPathPrefix)) {
      logger.info().status(Status.IMPORTANT).scene("Controller").log("系统 API 请求路径统一前缀为：$apiPathPrefix")
      configurer.addPathPrefix(apiPathPrefix) { true }
    }
  }

  companion object {
    private val logger = logger { }
  }
}