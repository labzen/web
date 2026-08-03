package cn.labzen.web.log;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.annotation.Nonnull;

import static cn.labzen.web.api.definition.Constants.*;

/**
 * API 日志响应增强处理器。
 * <p>
 * 在 Spring MVC 将响应体写入输出流之前拦截，打印响应日志。
 * 与 {@link ApiLogInterceptor} 配合，通过请求属性传递配置和元数据，避免重复计算。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *   <li>从请求属性中读取拦截器阶段缓存的配置和元数据</li>
 *   <li>计算请求处理耗时</li>
 *   <li>调用 {@link ApiLogMessageBuilder} 打印响应日志（含脱敏、截断）</li>
 * </ul>
 *
 * @see ApiLogInterceptor
 * @see ApiLogMessageBuilder
 */
@RestControllerAdvice
public class ApiLogResponseAdvice implements ResponseBodyAdvice<Object> {

  private final ApiLogMessageBuilder messageBuilder;

  public ApiLogResponseAdvice(ApiLogMessageBuilder messageBuilder) {
    this.messageBuilder = messageBuilder;
  }

  @Override
  public boolean supports(
    @Nonnull MethodParameter returnType,
    @Nonnull Class<? extends HttpMessageConverter<?>> converterType
  ) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
    Object body,
    @Nonnull MethodParameter returnType,
    @Nonnull MediaType selectedContentType,
    @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
    @Nonnull ServerHttpRequest request,
    @Nonnull ServerHttpResponse response
  ) {
    HttpServletRequest httpRequest = null;
    HttpServletResponseWrapper responseWrapper = null;

    if (request instanceof ServletServerHttpRequest servletRequest) {
      httpRequest = servletRequest.getServletRequest();
    }
    if (response instanceof ServletServerHttpResponse servletResponse) {
      responseWrapper = new HttpServletResponseWrapper(servletResponse.getServletResponse());
    }

    if (httpRequest == null) {
      return body;
    }

    try {
      // 从请求属性中获取拦截器阶段缓存的配置和元数据
      ControllerMeta controllerMeta =
        (ControllerMeta) httpRequest.getAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE);

      if (controllerMeta == null) {
        return body;
      }

      ApiEndpointLogConfig effectiveConfig = (ApiEndpointLogConfig) httpRequest.getAttribute(API_LOG_CONFIG_ATTRIBUTE);
      if (effectiveConfig == null) {
        return body; // 拦截器阶段跳过了日志打印，此处也跳过
      }

      // 确定 Controller 实现类
      Object handler = httpRequest.getAttribute(
        "org.springframework.web.servlet.HandlerMapping.bestMatchingHandler");
      Class<?> controllerClass = handler != null ? handler.getClass() : null;

      if (controllerClass == null) {
        return body;
      }

      // 计算耗时
      long startTime = getRequestStartTime(httpRequest);
      long costMs = System.currentTimeMillis() - startTime;

      // 获取状态码
      int statusCode = responseWrapper != null ? responseWrapper.getStatus() : 200;

      // 打印响应日志
      messageBuilder.logResponse(controllerClass, effectiveConfig, statusCode, body, costMs);

    } catch (Exception e) {
      // 响应日志打印异常不影响正常业务流程
    }

    return body;
  }

  private long getRequestStartTime(HttpServletRequest request) {
    Object millis = request.getAttribute(REST_REQUEST_TIME_MILLIS);
    if (millis instanceof Long l) {
      return l;
    }
    return System.currentTimeMillis();
  }

  private static class HttpServletResponseWrapper {
    private final jakarta.servlet.http.HttpServletResponse delegate;

    HttpServletResponseWrapper(jakarta.servlet.http.HttpServletResponse delegate) {
      this.delegate = delegate;
    }

    int getStatus() {
      return delegate.getStatus();
    }
  }
}
