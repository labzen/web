package cn.labzen.web.response.format;

import cn.labzen.web.api.definition.HttpStatusExt;
import cn.labzen.web.api.response.Response;
import com.google.common.primitives.Ints;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Optional;

/**
 * 格式化不正常的 Http Status 结果，如404
 */
public class AbnormalStatusResponseFormatter implements ResponseFormatter {

  private static final int UNEXPECTED_ERROR = HttpStatusExt.UNEXPECTED_ERROR.code();

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return Map.class.isAssignableFrom(clazz) && request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    Map<String, Object> data = (Map<String, Object>) result;
    String status = data.getOrDefault("status", "").toString();
    Integer code = Optional.ofNullable(Ints.tryParse(status)).orElse(UNEXPECTED_ERROR);
    String message = data.getOrDefault("error", "").toString();
    return new Response(code, message, null, null);
  }
}
