package cn.labzen.web.response.format;

import cn.labzen.web.api.paging.Pagination;
import cn.labzen.web.api.response.out.Meta;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.api.response.result.ValueResult;
import com.google.common.primitives.Longs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME;
import static cn.labzen.web.api.definition.Constants.REST_REQUEST_TIME_MILLIS;

/**
 * 标准结果格式化器
 * <p>
 * 处理 {@link ValueResult} 类型的返回值，将其转换为标准响应结构。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>提取 ValueResult 中的 code、message、value</li>
 *   <li>计算请求执行时长</li>
 *   <li>如果是分页数据，分离分页信息和记录列表</li>
 * </ul>
 */
public class StandardResultResponseFormatter implements ResponseFormatter {

  /**
   * 仅支持 ValueResult 类型
   */
  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return ValueResult.class.isAssignableFrom(clazz);
  }

  /**
   * 格式化 ValueResult
   * <p>
   * 构建包含执行时长、元数据、分页信息和实际数据的完整响应。
   */
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    String requestTime = request.getAttribute(REST_REQUEST_TIME).toString();
    String requestMillsStr = request.getAttribute(REST_REQUEST_TIME_MILLIS).toString();
    long requestMills = Optional.ofNullable(Longs.tryParse(requestMillsStr)).orElse(0L);
    long executionTime = System.currentTimeMillis() - requestMills;

    ValueResult data = (ValueResult) result;

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
