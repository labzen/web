package cn.labzen.web.api.log.registry;

import java.util.List;

/**
 * 方法元数据记录。
 *
 * @param methodName     方法名（如 "create"）
 * @param httpMethod     HTTP 方法（如 "POST"）
 * @param urlPattern     URL 模式（如 "/api/user"）
 * @param fullUrlPattern 完整 URL 模式（含类级别路径前缀）
 * @param parameterTypes 参数类型的简单名称列表
 */
public record ControllerMethodMeta(
  String methodName,
  String httpMethod,
  String urlPattern,
  String fullUrlPattern,
  List<String> parameterTypes
) {
}