package cn.labzen.web.spring;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.log.*;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.spring.runtime.LabzenExceptionCatchingFilter;
import cn.labzen.web.spring.runtime.LabzenHandlerExceptionResolver;
import cn.labzen.web.spring.runtime.LabzenRestResponseBodyAdvice;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class LabzenWebComponentRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(@Nonnull AnnotationMetadata importingClassMetadata,
                                      @Nonnull BeanDefinitionRegistry registry) {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);

    // 注册转换 Http Response 结构的组件
    if (configuration.responseFormattingEnabled()) {
      register(registry, LabzenRestResponseBodyAdvice.class);
    }
    // 注册异常处理组件
    register(registry, LabzenExceptionCatchingFilter.class);
    register(registry, LabzenHandlerExceptionResolver.class);

    // -------------- 注册 API 日志输出相关组件 --------------------------
    // 注册 Labzen Web Controller 元数据注册器组件
    register(registry, LoggableControllerMetaRegistry.class);
    // 注册 API 日志配置管理器组件
    register(registry, ApiLogConfigManager.class);
    // 注册 API 日志消息构建器组件
    register(registry, ApiLogMessageBuilder.class);
    // 注册 API 日志请求拦截器组件
    register(registry, ApiLogInterceptor.class);
    // 注册 API 日志响应体捕获器（在 LabzenRestResponseBodyAdvice 之后执行）
    register(registry, ApiLogResponseAdvice.class);
  }

  private void register(BeanDefinitionRegistry registry, Class<?> clazz) {
    String simpleName = clazz.getSimpleName();
    String beanName = Strings.camelCase(simpleName);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(clazz);
    registry.registerBeanDefinition(beanName, beanDefinition);
  }
}
