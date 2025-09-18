package cn.labzen.web.response.format;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 响应内容格式化接口，处理Result结果中的数据
 */
public interface ResponseFormatter {

  boolean support(Class<?> clazz, HttpServletRequest request);

  Object format(Object result, HttpServletRequest request, HttpServletResponse response);
}
