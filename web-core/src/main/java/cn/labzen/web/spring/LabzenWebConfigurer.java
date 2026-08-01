package cn.labzen.web.spring;

import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.meta.Labzens;
import cn.labzen.web.api.log.registry.LoggableControllerMetaRegistry;
import cn.labzen.web.log.*;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.spring.runtime.LabzenExceptionCatchingFilter;
import cn.labzen.web.spring.runtime.LabzenHandlerExceptionResolver;
import cn.labzen.web.spring.runtime.LabzenRestRequestHandlerInterceptor;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

import static cn.labzen.web.api.definition.Constants.LOGGER_SCENE_CONTROLLER;

@Slf4j
public class LabzenWebConfigurer implements WebMvcConfigurer {

  @Override
  public void addInterceptors(@Nonnull InterceptorRegistry registry) {
    // 注册 API 日志拦截器（在 RestRequestHandlerInterceptor 之前执行）
    registry.addInterceptor(apiLogInterceptor());
    registry.addInterceptor(new LabzenRestRequestHandlerInterceptor());
  }

  /**
   * 创建 API 日志拦截器 Bean。
   * <p>
   * 通过 SPI 加载 {@link LoggableControllerMetaRegistry} 实现（由 APT 编译期生成）。
   * 若未找到实现（如项目中无 @LabzenController 接口），使用空注册表降级。
   */
  @Bean
  public ApiLogInterceptor apiLogInterceptor() {
    ApiLogConfigManager configManager = apiLogConfigManager();

    // 通过 SPI 加载编译期生成的注册表实现
    LoggableControllerMetaRegistry registry = apiLogControllerRegistry();

    ApiLogConditionEvaluator evaluator = new ApiLogConditionEvaluator();
    ApiLogMessageBuilder messageBuilder = new ApiLogMessageBuilder();

    return new ApiLogInterceptor(configManager, registry, evaluator, messageBuilder);
  }

  /**
   * 创建 API 日志配置管理器 Bean。
   * <p>
   * 同时将 {@link LoggableControllerMetaRegistry} 注入，以便 {@code getApiEndpointsDetail()}
   * 方法提供 API 端点详情查询功能。
   */
  @Bean
  public ApiLogConfigManager apiLogConfigManager() {
    ApiLogConfigManager configManager = new ApiLogConfigManager();
    configManager.setRegistry(apiLogControllerRegistry());
    return configManager;
  }

  /**
   * 创建 API 日志 Controller 元数据注册表 Bean。
   * <p>
   * 通过 SPI 加载 APT 编译期生成的实现，若未找到则降级为空实现。
   */
  @Bean
  public LoggableControllerMetaRegistry apiLogControllerRegistry() {
    return ServiceLoader.load(LoggableControllerMetaRegistry.class, getClass().getClassLoader())
        .findFirst()
        .orElse(new EmptyLoggableControllerMetaRegistry());
  }

  /**
   * 空注册表降级实现（当无 APT 生成的注册表时使用）。
   */
  private static class EmptyLoggableControllerMetaRegistry implements LoggableControllerMetaRegistry {
    @Override
    public java.util.Map<String, ControllerMeta> getAllMetas() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Optional<ControllerMeta> lookup(String controllerSimpleName) {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<MethodMeta> lookupMethod(String controllerSimpleName, String methodKey) {
      return java.util.Optional.empty();
    }
  }

  /**
   * 定义API的前缀等
   */
  @Override
  public void configurePathMatch(@Nonnull PathMatchConfigurer configurer) {
    configurer.setUrlPathHelper(new UrlPathHelper());

    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);

    String apiPathPrefix = configuration.apiPathPrefix();
    if (!Strings.isNullOrEmpty(apiPathPrefix)) {
      ((LabzenLogger) logger).atInfo().status(Status.IMPORTANT).scene(LOGGER_SCENE_CONTROLLER)
        .log("系统 API 请求路径统一前缀为：'/" + apiPathPrefix + "'");

      configurer.addPathPrefix(apiPathPrefix, predicate -> true);
    }
  }

  /**
   * 注册异常捕捉过滤器
   */
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> filterRegistrationBean() {
    FilterRegistrationBean<OncePerRequestFilter> filterRegistration = new FilterRegistrationBean<>();
    filterRegistration.setFilter(new LabzenExceptionCatchingFilter());
    filterRegistration.addUrlPatterns("/*");
    filterRegistration.setOrder(Integer.MIN_VALUE);
    return filterRegistration;
  }

  @Bean
  public HandlerExceptionResolver labzenHandlerExceptionResolver() {
    return new LabzenHandlerExceptionResolver();
  }

  /**
   * 扩展异常处理解析器
   */
  @Override
  public void extendHandlerExceptionResolvers(@Nonnull List<HandlerExceptionResolver> resolvers) {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    if (configuration.responseFormattingForcedAll()) {
      OptionalInt found = IntStream.range(0, resolvers.size()).filter(i -> resolvers.get(i) instanceof DefaultHandlerExceptionResolver).findFirst();
      found.ifPresent(i -> {
        resolvers.add(i, labzenHandlerExceptionResolver());
      });
    }
  }
}
