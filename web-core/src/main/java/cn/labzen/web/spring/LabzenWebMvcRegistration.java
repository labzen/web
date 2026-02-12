package cn.labzen.web.spring;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.meta.WebConfiguration;
import cn.labzen.web.spring.runtime.LabzenVersionedApiRequestMappingHandlerMapping;
import cn.labzen.web.spring.runtime.PageableCompatibleArgumentResolver;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

public class LabzenWebMvcRegistration implements WebMvcRegistrations {

  @Bean
  public InitializingBean initPageableArgumentResolver(RequestMappingHandlerAdapter adapter) {
    return () -> {
      List<HandlerMethodArgumentResolver> argumentResolvers = adapter.getArgumentResolvers();
      if (argumentResolvers == null) {
        argumentResolvers = new ArrayList<>();
      }

      List<HandlerMethodArgumentResolver> newResolversCollection = Lists.newArrayList(new PageableCompatibleArgumentResolver());
      newResolversCollection.addAll(argumentResolvers);
      adapter.setArgumentResolvers(newResolversCollection);
    };
  }

  /**
   * 注册自定义的 {@link RequestMappingHandlerMapping} 实现API的版本控制
   */
  @Override
  public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
    WebConfiguration configuration = Labzens.configurationWith(WebConfiguration.class);
    if (configuration.apiVersionCarrier() == APIVersionCarrier.URI) {
      return new LabzenVersionedApiRequestMappingHandlerMapping();
    } else {
      return null;
    }
  }
}
