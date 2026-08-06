package cn.labzen.web.log.bean;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;

import java.util.List;

/**
 * API 端点详情，用于管理页面展示单个端点的日志配置状态。
 *
 * @param httpMethod     HTTP 方法（GET/POST/PUT/DELETE 等）
 * @param urlPattern     URL 路径模式，如 {@code /api/user/{id}}
 * @param methodName     Controller 接口中的方法名
 * @param methodHash     方法签名哈希（用于快速查找）
 * @param controllerName Controller 接口名
 * @param parameterTypes 方法参数类型列表（全限定名）
 * @param logConfig      当前生效的端点日志配置
 */
public record ApiEndpointDetail(String httpMethod, String urlPattern, String methodName, String methodHash,
                                String controllerName, List<String> parameterTypes, ApiEndpointLogConfig logConfig) {

}
