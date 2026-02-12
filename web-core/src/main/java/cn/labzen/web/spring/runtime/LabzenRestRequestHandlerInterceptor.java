package cn.labzen.web.spring.runtime;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.DateTimes;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.meta.WebConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME;
import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME_MILLIS;

/**
 * 拦截器，用于统计请求处理时长、以及其他与 Labzen Web 组件相关的功能处理
 */
public class LabzenRestRequestHandlerInterceptor implements HandlerInterceptor {

  private final Supplier<Boolean> forceRequestWithVersionHeader = () -> {
    var configuration = Labzens.configurationWith(WebConfiguration.class);
    return configuration.apiVersionCarrier() == APIVersionCarrier.HEADER
      && configuration.apiVersionHeaderAcceptForced();
  };

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