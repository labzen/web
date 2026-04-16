package cn.labzen.web.response.format;

import cn.labzen.web.api.response.out.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 直接返回格式化器
 * <p>
 * 如果 Controller 返回值已经是 {@link Response} 结构，直接返回不做处理。
 * 这是第一个被检查的格式化器，用于快速路径。
 */
public class ResponseAgainResponseFormatter implements ResponseFormatter {

  /**
   * 支持所有 Response 类型
   */
  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return Response.class.isAssignableFrom(clazz);
  }

  /**
   * 直接返回原值
   */
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    return result;
  }
}
