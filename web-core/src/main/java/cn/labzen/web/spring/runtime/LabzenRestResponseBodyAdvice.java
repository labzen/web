package cn.labzen.web.spring.runtime;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.response.Response;
import cn.labzen.web.api.response.Result;
import cn.labzen.web.exception.RequestException;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.response.format.CompositeResponseFormatter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Nonnull;

/**
 * 对 Controller 返回的响应结果进行增强处理（转换 Http Response 结构）
 */
@RestControllerAdvice
public class LabzenRestResponseBodyAdvice implements ResponseBodyAdvice<Object>, InitializingBean {

  private final CompositeResponseFormatter responseFormatter = new CompositeResponseFormatter();
  private boolean processAllRestResponse = true;

  @Override
  public void afterPropertiesSet() {
    var configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    processAllRestResponse = configuration.responseFormattingForcedAll();
  }

  @Override
  public boolean supports(@Nonnull MethodParameter returnType,
                          @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return processAllRestResponse || returnType.getParameterType() == Result.class;
  }

  @Override
  public Object beforeBodyWrite(Object body,
                                @Nonnull MethodParameter returnType,
                                @Nonnull MediaType selectedContentType,
                                @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                @Nonnull ServerHttpRequest request,
                                @Nonnull ServerHttpResponse response) {
    HttpServletRequest httpRequest = (request instanceof ServletServerHttpRequest servletRequest)
      ? servletRequest.getServletRequest()
      : null;
    HttpServletResponse httpResponse = (response instanceof ServletServerHttpResponse servletResponse)
      ? servletResponse.getServletResponse()
      : null;

    if (httpRequest == null || httpResponse == null) {
      return body;
    }

    return responseFormatter.format(body, httpRequest, httpResponse);
  }

  /**
   * #第1级异常拦截处理：在这里处理业务相关异常，推荐统一封装为 RequestException !!
   */
  @ExceptionHandler(RequestException.class)
  public Object handleLabzenRequestException(HttpServletRequest request,
                                             HttpServletResponse response,
                                             RequestException e) {
    return new Response(e.getCode(), e.getMessage() != null ? e.getMessage() : "internal server error", null, null);
  }
}