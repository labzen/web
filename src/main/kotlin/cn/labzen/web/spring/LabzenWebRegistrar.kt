package cn.labzen.web.spring

import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.meta.Labzens
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.source.ControllerClassInitializer
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata

class LabzenWebRegistrar : ImportBeanDefinitionRegistrar {

  private val logger = logger { }

  override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)

    if (configuration.unifyRestResponse()) {
      // 注册 ResponseBodyAdvice [LabzenRestResponseBody]
      registry.registerBeanDefinition("labzenRestResponseBody", RootBeanDefinition(LabzenRestResponseBody::class.java))
    }

    // 注册在 [LabzenWebInitializer] 中生成好的 Controller 类
    ControllerClassInitializer.controllerClasses.forEach {
      logger.info().status(Status.IMPORTANT).scene("Controller")
        .log("Register dynamic mvc controller bean for [$it]")
      val beanDefinition = RootBeanDefinition(it)

      val name = it.simpleName.first().lowercase() + it.simpleName.substring(1)
      registry.registerBeanDefinition(name, beanDefinition)
    }
  }
}