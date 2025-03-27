package cn.labzen.web.spring

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.meta.Labzens
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.spring.runtime.LabzenExceptionCatcherFilter
import cn.labzen.web.spring.runtime.LabzenHandlerExceptionResolver
import cn.labzen.web.spring.runtime.LabzenResourceMessageConverter
import cn.labzen.web.spring.runtime.LabzenRestRequestHandlerInterceptor
import com.google.common.base.Strings
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.ResourceHttpMessageConverter
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
import org.springframework.web.util.UrlPathHelper

class LabzenWebConfigurer : WebMvcConfigurer {

  /**
   * 注册拦截器
   */
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(LabzenRestRequestHandlerInterceptor())
  }

  /**
   * 定义API的前缀等
   */
  override fun configurePathMatch(configurer: PathMatchConfigurer) {
    configurer.setUrlPathHelper(UrlPathHelper())

    val configuration = Labzens.configurationWith(WebConfiguration::class.java)

    val apiPathPrefix = configuration.apiPathPrefix()
    if (!Strings.isNullOrEmpty(apiPathPrefix)) {
      logger.info().status(Status.IMPORTANT).scene(LOGGER_SCENE_CONTROLLER)
        .log("系统 API 请求路径统一前缀为：$apiPathPrefix")
      configurer.addPathPrefix(apiPathPrefix) { true }
    }
  }

  /**
   * 注册异常捕捉过滤器
   */
  @Bean
  fun filterRegistrationBean(): FilterRegistrationBean<OncePerRequestFilter> {
    val filterRegistration = FilterRegistrationBean<OncePerRequestFilter>()
    filterRegistration.filter = LabzenExceptionCatcherFilter()
    filterRegistration.addUrlPatterns("/*")
    filterRegistration.order = Int.MIN_VALUE

    return filterRegistration
  }

  @Bean
  fun labzenHandlerExceptionResolver(): HandlerExceptionResolver =
    LabzenHandlerExceptionResolver()

  /**
   * 扩展异常处理解析器
   */
  override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    if (configuration.unifyAllRestResponse()) {
      val index = resolvers.indexOfFirst { it is DefaultHandlerExceptionResolver }
      resolvers.add(index, labzenHandlerExceptionResolver())
    }
  }

  override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    val resourceConverterIndex = converters.indexOfFirst { it is ResourceHttpMessageConverter }
    converters.add(if (resourceConverterIndex >= 0) resourceConverterIndex else 0, labzenResourceMessageConverter())
  }

  @Bean
  fun labzenResourceMessageConverter(): LabzenResourceMessageConverter =
    LabzenResourceMessageConverter()

  companion object {
    private val logger = logger { }
  }
}