package cn.labzen.web.spring;

import cn.labzen.meta.Labzens;
import cn.labzen.web.log.ApiLogMessageBuilder;
import cn.labzen.web.log.ApiLogResponseAdvice;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.spring.runtime.LabzenRestResponseBodyAdvice;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import jakarta.annotation.Nonnull;

public class LabzenWebComponentRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(@Nonnull AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);

    // 注册转换 Http Response 结构的组件
    if (configuration.responseFormattingEnabled()) {
      // 注册 ResponseBodyAdvice [labzenRestResponseBodyAdvice]
      registry.registerBeanDefinition("labzenRestResponseBodyAdvice", new RootBeanDefinition(LabzenRestResponseBodyAdvice.class));
    }

    // 注册 API 日志消息构建器（Singleton，供拦截器和 Advice 共用）
    registry.registerBeanDefinition("apiLogMessageBuilder", new RootBeanDefinition(ApiLogMessageBuilder.class));

    // 注册 API 日志响应增强处理器
    registry.registerBeanDefinition("apiLogResponseAdvice", new RootBeanDefinition(ApiLogResponseAdvice.class));
  }
}
