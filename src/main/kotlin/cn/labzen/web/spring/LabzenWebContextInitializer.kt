package cn.labzen.web.spring

import cn.labzen.logger.kotlin.logger
import cn.labzen.meta.Labzens
import cn.labzen.meta.spring.SpringApplicationContextInitializerOrder
import cn.labzen.spring.Springs
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.paging.PageConverter
import cn.labzen.web.paging.converter.NonePageConverter
import cn.labzen.web.paging.converter.PageConverterHolder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered

class LabzenWebContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

  override fun getOrder(): Int =
    SpringApplicationContextInitializerOrder.MODULE_WEB_INITIALIZER_ORDER

  /**
   * 初始化 Web 组件内的必要数据
   */
  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    val pageConverterFQCN = configuration.pageConverter().ifBlank { NONE_PAGE_CONVERTER_FQCN }

    try {
      val pageConverterClass: Class<*> = Class.forName(pageConverterFQCN)
      if (PageConverter::class.java.isAssignableFrom(pageConverterClass)) {
        val converter = Springs.getOrCreate(pageConverterClass) as PageConverter<*>
        PageConverterHolder.converter = converter
      }
    } catch (e: Exception) {
      logger.error("初始化 PageConverter 实例异常", e)
    }
  }

  companion object {
    private val NONE_PAGE_CONVERTER_FQCN = NonePageConverter::class.java.name
    private val logger = logger { }
  }
}