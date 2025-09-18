package cn.labzen.web.response.format;

import cn.labzen.web.response.bean.Meta;
import cn.labzen.web.response.bean.Response;
import cn.labzen.web.response.bean.Result;
import com.google.common.primitives.Longs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

import static cn.labzen.web.defination.Constants.REST_REQUEST_TIME;
import static cn.labzen.web.defination.Constants.REST_REQUEST_TIME_MILLIS;

/**
 * 格式化非 {@link Result} 类型的返回值
 */
public class UnexpectedResponseFormatter implements ResponseFormatter {

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return true;
  }

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
