package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.paging.internal.PageableResolver;
import cn.labzen.web.paging.internal.PageableSupport;
import cn.labzen.web.paging.internal.Paging;
import jakarta.annotation.Nonnull;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * 请求体分页兼容处理。
 * <p>
 * 请求体绑定的 {@link Pageable} 资源在反序列化完成后附加分页能力。分页条件按 query string 优先、
 * 请求体次之的顺序解析：请求体中的 {@code unpaged}、{@code pageNumber}/{@code pn}、
 * {@code pageSize}/{@code ps}、{@code orders}/{@code od} 会被提取出来放进请求属性，
 * 由 {@link PageableResolver} 统一读取。
 * <p>
 * 请求体为空时，为具体资源类型创建空实例并附加分页能力 —— 空请求体与 {@code {}} 等价，
 * 调用方拿到的是带分页能力的空资源。资源类型为接口、抽象类，或没有可访问默认构造器时，
 * 仍按 {@code null} 传递。
 */
@ControllerAdvice
public class LabzenPageableRequestBodyAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(MethodParameter methodParameter,
                          @Nonnull Type targetType,
                          @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return Pageable.class.isAssignableFrom(methodParameter.getParameterType());
  }

  /**
   * 缓存请求体并提取其中的分页条件
   * <p>
   * 请求体被完整读出后以字节数组重新提供，后续消息转换器仍能正常反序列化。
   */
  @Nonnull
  @Override
  public HttpInputMessage beforeBodyRead(@Nonnull HttpInputMessage inputMessage,
                                         @Nonnull MethodParameter parameter,
                                         @Nonnull Type targetType,
                                         @Nonnull Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());

    NativeWebRequest webRequest = currentWebRequest();
    if (webRequest != null && body.length > 0) {
      Paging paging = PageableResolver.resolveFromBody(body);
      if (paging != null) {
        webRequest.setAttribute(PageableResolver.PAGING_ATTRIBUTE, paging, RequestAttributes.SCOPE_REQUEST);
      }
    }

    return new BodyHttpInputMessage(inputMessage.getHeaders(), body);
  }

  @Nonnull
  @Override
  public Object afterBodyRead(@Nonnull Object body,
                              @Nonnull HttpInputMessage inputMessage,
                              @Nonnull MethodParameter parameter,
                              @Nonnull Type targetType,
                              @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return PageableSupport.attachPaging(parameter, body, currentWebRequest());
  }

  @Override
  public Object handleEmptyBody(Object body,
                                @Nonnull HttpInputMessage inputMessage,
                                @Nonnull MethodParameter parameter,
                                @Nonnull Type targetType,
                                @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    Object resource = body != null ? body : newResource(parameter.getParameterType());
    return PageableSupport.attachPaging(parameter, resource, currentWebRequest());
  }

  /**
   * 为请求体为空的情况创建资源实例
   * <p>
   * 只处理可实例化的具体类型：接口、抽象类、非 {@link Pageable} 类型，以及没有可访问默认构造器的类型
   * 一律返回 {@code null}，由调用方按"未提供资源"处理。
   */
  private Object newResource(Class<?> resourceType) {
    if (!Pageable.class.isAssignableFrom(resourceType)
        || resourceType.isInterface()
        || Modifier.isAbstract(resourceType.getModifiers())) {
      return null;
    }
    try {
      return BeanUtils.instantiateClass(resourceType);
    } catch (BeanInstantiationException e) {
      return null;
    }
  }

  /**
   * 取当前线程绑定的 Web 请求
   * <p>
   * 消息转换阶段传给 advice 的 {@code HttpInputMessage} 会被框架包装，不能按具体实现类判断；
   * {@code DispatcherServlet} 绑定到请求上下文的是 {@link ServletRequestAttributes}，据此构造
   * {@link ServletWebRequest}。
   */
  private NativeWebRequest currentWebRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof NativeWebRequest webRequest) {
      return webRequest;
    }
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      return new ServletWebRequest(servletAttributes.getRequest(), servletAttributes.getResponse());
    }
    return null;
  }

  /**
   * 以字节数组重复提供请求体
   */
  private record BodyHttpInputMessage(HttpHeaders headers, byte[] body) implements HttpInputMessage {

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }

    @Nonnull
    @Override
    public InputStream getBody() {
      return new ByteArrayInputStream(body);
    }
  }
}
