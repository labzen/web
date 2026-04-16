package cn.labzen.web.response.format;

import cn.labzen.web.api.response.out.Meta;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.api.response.result.Result;
import com.google.common.primitives.Longs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME;
import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME_MILLIS;

/**
 * 兜底格式化器
 * <p>
 * 处理所有未被其他格式化器处理的返回值类型。
 * 将任意类型的返回值包装成标准响应结构。
 */
public class UnexpectedResponseFormatter implements ResponseFormatter {

  /**
   * 接受所有类型
   */
  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return true;
  }

  /**
   * 包装为标准响应
   */
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    String requestTime = request.getAttribute(REST_REQUEST_TIME).toString();
    String requestMillsStr = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString();
    long requestMills = Optional.ofNullable(Longs.tryParse(requestMillsStr)).orElse(0L);
    long executionTime = System.currentTimeMillis() - requestMills;

    Meta meta = new Meta(requestTime, executionTime, null, null, null);
    return new Response(200, "success", meta, result);
  }
}
