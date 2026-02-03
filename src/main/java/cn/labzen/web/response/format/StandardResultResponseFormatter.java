package cn.labzen.web.response.format;

import cn.labzen.web.paging.Pagination;
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
 * 处理 {@link Result} 中的返回值格式化，通用的格式化器，在 {@link CompositeResponseFormatter} 中的执行顺序最晚，如果其他的格式化器不处理返回值，则在这里格式化，算是最后托底的
 */
public class StandardResultResponseFormatter implements ResponseFormatter {

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return Result.class.isAssignableFrom(clazz);
  }

  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    String requestTime = request.getAttribute(REST_REQUEST_TIME).toString();
    String requestMillsStr = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString();
    long requestMills = Optional.ofNullable(Longs.tryParse(requestMillsStr)).orElse(0L);
    long executionTime = System.currentTimeMillis() - requestMills;

    Result data = (Result) result;

    int code = data.code();
    String message = Optional.ofNullable(data.message()).orElse("success");
    Object value = data.value();

    if (value instanceof Pagination<?> pagination) {
      Meta meta = new Meta(requestTime, executionTime, pagination.pageable() ? pagination.copyWithoutRecords() : null, null, null);
      return new Response(code, message, meta, pagination.records());
    } else {
      Meta meta = new Meta(requestTime, executionTime, null, null, null);
      return new Response(code, message, meta, value);
    }
  }
}
