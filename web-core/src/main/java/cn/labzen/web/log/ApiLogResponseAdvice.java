package cn.labzen.web.log;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static cn.labzen.web.api.definition.Constants.API_LOG_CONFIG_ATTRIBUTE;
import static cn.labzen.web.api.definition.Constants.RESPONSE_RESULT_BODY_ATTRIBUTE;

/**
 * API 日志响应体捕获器。
 * <p>
 * 在 Spring MVC 将响应体写入输出流之前拦截，捕获原始响应体并存入 request attribute，
 * 供 {@link ApiLogInterceptor#postHandle} 阶段传递给 {@link ApiLogMessageBuilder} 输出日志。
 * <p>
 * <b>职责边界：</b>只负责捕获原始响应体，不做任何处理。所有日志消息构建（包括响应体处理）
 * 统一由 {@link ApiLogMessageBuilder} 完成。
 * <p>
 * <b>执行顺序：</b>{@link Order} 值为 {@link Ordered#LOWEST_PRECEDENCE} + 1，
 * 确保在 {@code LabzenRestResponseBodyAdvice}（默认 LOWEST_PRECEDENCE）之后执行，
 * 从而捕获到格式化后的标准响应结构。
 *
 * @see ApiLogInterceptor
 * @see ApiLogMessageBuilder
 */
@RestControllerAdvice
@Order(2_000)
public class ApiLogResponseAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(@Nonnull MethodParameter returnType,
                          @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body,
                                @Nonnull MethodParameter returnType,
                                @Nonnull MediaType selectedContentType,
                                @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                @Nonnull ServerHttpRequest request,
                                @Nonnull ServerHttpResponse response) {
    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      return body;
    }

    HttpServletRequest httpRequest = servletRequest.getServletRequest();

    // 仅当拦截器阶段已确定需要打印响应日志时才捕获 body
    Object configAttr = httpRequest.getAttribute(API_LOG_CONFIG_ATTRIBUTE);
    if (configAttr instanceof ApiEndpointLogConfig config && Boolean.TRUE.equals(config.getLogResponse())) {
      // 原始响应体直接存入 request attribute，由 ApiLogMessageBuilder 统一处理
      httpRequest.setAttribute(RESPONSE_RESULT_BODY_ATTRIBUTE, body);
    }

    return body;
  }
}
