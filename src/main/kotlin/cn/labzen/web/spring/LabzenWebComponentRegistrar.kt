package cn.labzen.web.spring

import cn.labzen.meta.Labzens
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.spring.runtime.LabzenRestResponseBodyAdvice
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata

class LabzenWebComponentRegistrar : ImportBeanDefinitionRegistrar {

  override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
    val configuration = Labzens.configurationWith(WebConfiguration::class.java)

    // 注册转换 Http Response 结构的组件
    if (configuration.responseFormattingEnabled()) {
      // 注册 ResponseBodyAdvice [labzenRestResponseBodyAdvice]
      registry.registerBeanDefinition(
        "labzenRestResponseBodyAdvice",
        RootBeanDefinition(LabzenRestResponseBodyAdvice::class.java)
      )
    }
  }
}