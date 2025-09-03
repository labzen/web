package cn.labzen.web.spring

import cn.labzen.meta.Labzens
import cn.labzen.web.defination.APIVersionCarrier
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.spring.runtime.LabzenVersionedApiRequestMappingHandlerMapping
import cn.labzen.web.spring.runtime.PageableArgumentResolver
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

class LabzenWebMvcRegistration : WebMvcRegistrations  {

  @Bean
  fun initPageableArgumentResolver(adapter: RequestMappingHandlerAdapter): InitializingBean {
    return InitializingBean {
      val resolvers = adapter.argumentResolvers?.toMutableList() ?: mutableListOf()
      resolvers.add(0, PageableArgumentResolver())
      adapter.argumentResolvers = resolvers
    }
  }

  /**
   * 注册自定义的 [RequestMappingHandlerMapping] 实现API的版本控制
   */
  override fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping? {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    return if (configuration.apiVersionCarrier() == APIVersionCarrier.URI
    ) {
      LabzenVersionedApiRequestMappingHandlerMapping()
    } else null
  }
}