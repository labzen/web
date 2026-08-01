package cn.labzen.web.api.log.registry;

import java.util.Map;
import java.util.Optional;

/**
 * API 日志 Controller 元数据注册表接口。
 * <p>
 * 由 APT 处理器 {@code MetadataGenerateProcessor} 在编译期生成实现类
 * {@code LoggableControllerMetaRegistryImpl}，将每个标注了 {@code @LabzenController}
 * 的接口的元信息（接口名、方法名、HTTP 方法、URL Pattern 等）静态注册为 Map。
 * <p>
 * 运行时通过 {@link #lookup(String)} 和 {@link #lookupMethod(String, String)} 实现
 * O(1) 查表，完全消除运行时反射开销。
 * <p>
 * <b>使用方式：</b>
 * <pre>{@code
 *   // 查找 Controller 元数据
 *   Optional<ControllerMeta> meta = registry.lookup("UserController");
 *
 *   // 按方法名查找方法元数据
 *   Optional<MethodMeta> methodMeta = registry.lookupMethod("UserController", "create");
 *
 *   // 按 HTTP方法+URL 查找方法元数据
 *   Optional<MethodMeta> methodMeta = registry.lookupMethod("UserController", "POST /api/user");
 * }</pre>
 *
 * @see ControllerMeta
 * @see ControllerMethodMeta
 */
public interface LoggableControllerMetaRegistry {

  /**
   * 获取所有 Controller 元数据的只读视图。
   *
   * @return Map&lt;Controller简单名, ControllerMeta&gt;
   */
  Map<String, ControllerMeta> getAllMetas();

  /**
   * 按 Controller 简单名查找元数据。
   *
   * @param controllerSimpleName Controller 接口简单名（如 "UserController"）
   * @return ControllerMeta，未找到时返回空
   */
  Optional<ControllerMeta> lookup(String controllerSimpleName);

  /**
   * 按 Controller 简单名 + 方法标识查找方法元数据。
   * <p>
   * {@code methodKey} 支持两种格式：
   * <ul>
   *   <li>方法名：如 {@code "create"}</li>
   *   <li>HTTP方法+URL：如 {@code "POST /api/user"}</li>
   * </ul>
   *
   * @param controllerSimpleName Controller 接口简单名
   * @param methodKey            方法标识
   * @return MethodMeta，未找到时返回空
   */
  Optional<ControllerMethodMeta> lookupMethod(String controllerSimpleName, String methodKey);
}
