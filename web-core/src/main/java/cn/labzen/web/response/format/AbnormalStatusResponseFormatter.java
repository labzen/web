package cn.labzen.web.response.format;

import cn.labzen.web.api.definition.HttpStatusExt;
import cn.labzen.web.api.response.out.Response;
import com.google.common.primitives.Ints;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Optional;

/**
 * 异常状态码格式化器
 * <p>
 * 处理 Spring MVC 产生的异常状态响应（如 404、500 等错误页面）。
 * 将这些异常响应转换为标准的 JSON 响应结构。
 */
public class AbnormalStatusResponseFormatter implements ResponseFormatter {

  private static final int UNEXPECTED_ERROR = HttpStatusExt.UNEXPECTED_ERROR.code();

  /**
   * 支持 Map 类型且包含错误状态码属性
   */
  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return Map.class.isAssignableFrom(clazz) && request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null;
  }

  /**
   * 提取错误状态码和消息，转换为标准响应
   */
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    @SuppressWarnings("unchecked") 
    Map<String, Object> data = (Map<String, Object>) result;
    String status = data.getOrDefault("status", "").toString();
    Integer code = Optional.ofNullable(Ints.tryParse(status)).orElse(UNEXPECTED_ERROR);
    String message = data.getOrDefault("error", "").toString();
    return new Response(code, message, null, null);
  }
}
