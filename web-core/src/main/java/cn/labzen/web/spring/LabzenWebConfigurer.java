package cn.labzen.web.spring;

import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.meta.Labzens;
import cn.labzen.web.meta.WebConfiguration;
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
import java.util.stream.IntStream;

import static cn.labzen.web.api.definition.Constants.LOGGER_SCENE_CONTROLLER;

@Slf4j
public class LabzenWebConfigurer implements WebMvcConfigurer {

  @Override
  public void addInterceptors(@Nonnull InterceptorRegistry registry) {
    registry.addInterceptor(new LabzenRestRequestHandlerInterceptor());
  }

  /**
   * 定义API的前缀等
   */
  @Override
  public void configurePathMatch(@Nonnull PathMatchConfigurer configurer) {
    configurer.setUrlPathHelper(new UrlPathHelper());

    WebConfiguration configuration = Labzens.configurationWith(WebConfiguration.class);

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
    WebConfiguration configuration = Labzens.configurationWith(WebConfiguration.class);
    if (configuration.responseFormattingForcedAll()) {
      OptionalInt found = IntStream.range(0, resolvers.size()).filter(i -> resolvers.get(i) instanceof DefaultHandlerExceptionResolver).findFirst();
      found.ifPresent(i -> {
        resolvers.add(i, labzenHandlerExceptionResolver());
      });
    }
  }
}
