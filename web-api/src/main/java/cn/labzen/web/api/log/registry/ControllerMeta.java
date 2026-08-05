package cn.labzen.web.api.log.registry;

import java.util.Map;

/**
 * Controller 元数据记录。
 *
 * @param interfaceClass 接口类
 * @param simpleName     接口简单名（如 "UserController"）
 * @param methods        方法元数据 Map（Key: 方法名或HTTP方法+URL）
 */
public record ControllerMeta(Class<?> interfaceClass, String simpleName, Map<String, ControllerMethodMeta> methods) {

}
