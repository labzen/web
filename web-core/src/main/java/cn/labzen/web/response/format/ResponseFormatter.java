package cn.labzen.web.response.format;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 响应格式化器接口
 * <p>
 * 负责将 Controller 返回值转换为标准化的响应结构。
 * 实现类需提供：
 * <ul>
 *   <li>support - 判断是否支持当前返回值类型</li>
 *   <li>format - 执行实际的格式化转换</li>
 * </ul>
 *
 * @see CompositeResponseFormatter
 */
public interface ResponseFormatter {

  /**
   * 判断是否支持格式化该类型
   *
   * @param clazz 返回值类型
   * @param request HTTP 请求
   * @return 是否支持
   */
  boolean support(Class<?> clazz, HttpServletRequest request);

  /**
   * 格式化返回值
   *
   * @param result 原始返回值
   * @param request HTTP 请求
   * @param response HTTP 响应
   * @return 格式化后的响应
   */
  Object format(Object result, HttpServletRequest request, HttpServletResponse response);
}
