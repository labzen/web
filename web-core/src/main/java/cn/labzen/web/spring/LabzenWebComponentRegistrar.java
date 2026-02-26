package cn.labzen.web.spring;

import cn.labzen.meta.Labzens;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.spring.runtime.LabzenRestResponseBodyAdvice;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Nonnull;

public class LabzenWebComponentRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(@Nonnull AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);

    // 注册转换 Http Response 结构的组件
    if (configuration.responseFormattingEnabled()) {
      // 注册 ResponseBodyAdvice [labzenRestResponseBodyAdvice]
      registry.registerBeanDefinition("labzenRestResponseBodyAdvice", new RootBeanDefinition(LabzenRestResponseBodyAdvice.class));
    }
  }
}
