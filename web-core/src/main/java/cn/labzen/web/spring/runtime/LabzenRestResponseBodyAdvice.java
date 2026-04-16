package cn.labzen.web.spring.runtime;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.api.response.result.Result;
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
 * REST 响应体增强处理
 * <p>
 * 在 ResponseBody 写入之前拦截，对响应内容进行格式化转换。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>统一响应结构：所有 Controller 返回值都会被包装成标准 Response 格式</li>
 *   <li>异常处理：捕获 RequestException 并转换为标准响应</li>
 *   <li>格式化器链：通过 CompositeResponseFormatter 处理不同类型的返回值</li>
 * </ul>
 *
 * @see CompositeResponseFormatter
 */
@RestControllerAdvice
public class LabzenRestResponseBodyAdvice implements ResponseBodyAdvice<Object>, InitializingBean {

  private final CompositeResponseFormatter responseFormatter = new CompositeResponseFormatter();
  private boolean processAllRestResponse = true;

  /**
   * 初始化配置
   * <p>
   * 从配置中读取是否强制处理所有响应。
   */
  @Override
  public void afterPropertiesSet() {
    var configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    processAllRestResponse = configuration.responseFormattingForcedAll();
  }

  /**
   * 判断是否支持当前返回值类型
   * <p>
   * 当强制格式化开启时处理所有响应，否则只处理 Result 类型。
   */
  @Override
  public boolean supports(@Nonnull MethodParameter returnType,
                          @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return processAllRestResponse || Result.class.isAssignableFrom(returnType.getParameterType());
  }

  /**
   * 在响应体写入之前进行格式化
   * <p>
   * 核心处理：将原始返回值通过格式化器链转换为标准响应结构。
   */
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
   * 处理业务异常
   * <p>
   * 捕获 Controller 中抛出的 RequestException，转换为标准响应结构。
   */
  @ExceptionHandler(RequestException.class)
  public Object handleLabzenRequestException(HttpServletRequest request,
                                             HttpServletResponse response,
                                             RequestException e) {
    return new Response(e.getCode(), e.getMessage() != null ? e.getMessage() : "internal server error", null, null);
  }
}