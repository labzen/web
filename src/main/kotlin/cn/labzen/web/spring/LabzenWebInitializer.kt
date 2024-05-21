package cn.labzen.web.spring

import cn.labzen.cells.core.utils.Strings
import cn.labzen.meta.Labzens
import cn.labzen.meta.spring.SpringApplicationContextInitializerOrder
import cn.labzen.spring.Springs
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.request.PagingCondition
import cn.labzen.web.request.PagingConditionConverter
import cn.labzen.web.response.Pagination
import cn.labzen.web.response.PaginationConverter
import cn.labzen.web.source.ControllerClassInitializer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered

/**
 * 扫描 Controller 接口
 */
class LabzenWebInitializer : ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

  override fun getOrder(): Int =
    SpringApplicationContextInitializerOrder.MODULE_WEB_INITIALIZER_ORDER

  /**
   * 第一时间扫描所有的 Controller 接口，并生成相应的 Controller 类
   */
  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    ControllerClassInitializer.scanAndGenerate(configuration)

    // 设置分页相关
    PagingCondition.defaultSize = configuration.defaultPageSize()
    if (Strings.isNotBlank(configuration.pageConverterForRequest())) {
      try {
        val pageConverterClass: Class<*> = Class.forName(configuration.pageConverterForRequest())
        if (PagingConditionConverter::class.java.isAssignableFrom(pageConverterClass)) {
          val converter = Springs.getOrCreate(pageConverterClass) as PagingConditionConverter<*>
          PagingCondition.converter = converter
        }
      } catch (e: Exception) {
        // do nothing
      }
    }
    if (Strings.isNotBlank(configuration.pageConverterForResponse())) {
      try {
        val pageConverterClass: Class<*> = Class.forName(configuration.pageConverterForResponse())
        if (PaginationConverter::class.java.isAssignableFrom(pageConverterClass)) {
          val converter = Springs.getOrCreate(pageConverterClass) as PaginationConverter<*>
          Pagination.converter = converter
        }
      } catch (e: Exception) {
        // do nothing
      }
    }
  }

}