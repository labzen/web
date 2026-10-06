package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.paging.internal.PageableSupport;
import jakarta.annotation.Nonnull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.lang.reflect.Type;

/**
 * 请求体分页兼容处理。
 * <p>
 * 请求体绑定的 {@link Pageable} 资源在反序列化完成后附加分页能力，使请求体提供过滤条件、
 * query string 提供分页条件（{@code paging=1,20} 紧凑格式，或 {@code pn}/{@code ps}/{@code od} 缩写）。
 * <p>
 * 请求体为空时不附加分页能力，实例按原样传递。
 */
@ControllerAdvice
public class LabzenPageableRequestBodyAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(MethodParameter methodParameter,
                          @Nonnull Type targetType,
                          @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return Pageable.class.isAssignableFrom(methodParameter.getParameterType());
  }

  @Nonnull
  @Override
  public HttpInputMessage beforeBodyRead(@Nonnull HttpInputMessage inputMessage,
                                         @Nonnull MethodParameter parameter,
                                         @Nonnull Type targetType,
                                         @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return inputMessage;
  }

  @Nonnull
  @Override
  public Object afterBodyRead(@Nonnull Object body,
                              @Nonnull HttpInputMessage inputMessage,
                              @Nonnull MethodParameter parameter,
                              @Nonnull Type targetType,
                              @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return PageableSupport.attachPaging(parameter, body, webRequest(inputMessage));
  }

  @Override
  public Object handleEmptyBody(Object body,
                                @Nonnull HttpInputMessage inputMessage,
                                @Nonnull MethodParameter parameter,
                                @Nonnull Type targetType,
                                @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return body;
  }

  private NativeWebRequest webRequest(HttpInputMessage inputMessage) {
    if (inputMessage instanceof ServletServerHttpRequest servletRequest) {
      return new ServletWebRequest(servletRequest.getServletRequest());
    }
    return null;
  }
}
