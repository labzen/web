package cn.labzen.web.response.format;

import cn.labzen.web.api.response.out.Meta;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.util.ControllerDisposeHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
//    Object timeAttr = request.getAttribute(REST_REQUEST_TIME);
//    String requestTime = Strings.value(timeAttr, "");
//    Object millsAttr = request.getAttribute(REST_REQUEST_TIME_MILLIS);
//    String requestMillsStr = Strings.value(millsAttr, "0");
//    long requestMills = Optional.ofNullable(Longs.tryParse(requestMillsStr)).orElse(0L);
//    long executionTime = System.currentTimeMillis() - requestMills;

    String requestTime = ControllerDisposeHelper.getRequestTime(request);
    long executionTime = ControllerDisposeHelper.calculateExecutionTime(request);

    Meta meta = new Meta(requestTime, executionTime, null, null, null);
    return new Response(200, "success", meta, result);
  }
}
