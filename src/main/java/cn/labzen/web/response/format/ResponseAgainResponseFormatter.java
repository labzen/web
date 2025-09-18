package cn.labzen.web.response.format;

import cn.labzen.web.response.bean.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 处理已经是 {@link Response} 结构化的情况
 */
public class ResponseAgainResponseFormatter implements ResponseFormatter {

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return Response.class.isAssignableFrom(clazz);
  }

  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    return result;
  }
}
