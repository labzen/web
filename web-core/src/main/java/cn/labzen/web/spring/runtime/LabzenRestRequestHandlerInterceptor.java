package cn.labzen.web.spring.runtime;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.DateTimes;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.meta.WebCoreConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME;
import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME_MILLIS;

/**
 * 请求拦截器
 * <p>
 * 在请求处理之前执行，用于：
 * <ul>
 *   <li>强制验证 API 版本 Header（当配置要求时）</li>
 *   <li>记录请求开始时间，用于统计处理时长</li>
 * </ul>
 */
public class LabzenRestRequestHandlerInterceptor implements HandlerInterceptor {

  /**
   * 判断是否强制要求版本 Header
   * <p>
   * 条件：版本携带方式为 HEADER 且启用了强制验证。
   */
  private final Supplier<Boolean> forceRequestWithVersionHeader = () -> {
    var configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    return configuration.apiVersionCarrier() == APIVersionCarrier.HEADER
      && configuration.apiVersionHeaderAcceptForced();
  };

  /**
   * 前置处理
   * <p>
   * <ul>
   *   <li>1. 验证 Accept Header 是否包含有效的 API 版本信息</li>
   *   <li>2. 记录请求开始时间</li>
   * </ul>
   */
  @Override
  public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
    // 强制请求的Header中带有Accept版本信息
    if (forceRequestWithVersionHeader.get()) {
      String acceptHeader = request.getHeader("Accept");
      if (acceptHeader == null || !Strings.startsWith(acceptHeader, false, "application/vnd")) {
        response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        response.setHeader("Content-Type", "text/plain;charset=UTF-8");
        try {
          response.getWriter().write("Accept header must be specified with a valid API version");
        } catch (Exception e) {
          // 处理异常
        }
        return false;
      }
    }

    // 用于统计一个请求的用时
    request.setAttribute(REST_REQUEST_TIME_MILLIS, System.currentTimeMillis());
    request.setAttribute(REST_REQUEST_TIME, DateTimes.formattedNow());
    return true;
  }
}