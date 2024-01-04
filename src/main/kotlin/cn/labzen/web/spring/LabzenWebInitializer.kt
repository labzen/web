package cn.labzen.web.spring

import cn.labzen.meta.Labzens
import cn.labzen.meta.spring.SpringApplicationContextInitializerOrder
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.source.ControllerClassInitializer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered

class LabzenWebInitializer : ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

  override fun getOrder(): Int =
    SpringApplicationContextInitializerOrder.MODULE_WEB_INITIALIZER_ORDER

  override fun initialize(applicationContext: ConfigurableApplicationContext) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)
    ControllerClassInitializer.scanAndGenerate(configuration)
  }

}