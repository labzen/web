package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.log.registry.ControllerMethodMeta;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static cn.labzen.web.api.definition.Constants.LOGGER_SCENE_API_LOG_INIT;

/**
 * API 日志 Controller 元数据注册表（final 类，单例模式）。
 * <p>
 * 在首次使用时，扫描 classpath 下 {@code META-INF/labzen/*.meta.json} 目录的所有 JSON 元数据文件，
 * 这些文件由 APT 处理器 {@code MetadataGenerateProcessor} 在编译期为每个 {@code @LabzenController}
 * 接口自动生成。加载后缓存在内存中，提供 O(1) 的静态 Map 查表。
 * <p>
 * <b>使用方式：</b>
 * <pre>{@code
 *   // 获取所有 Controller 元数据
 *   Map<String, ControllerMeta> all = registry.getAllMetas();
 *
 *   // 查找指定 Controller
 *   Optional<ControllerMeta> meta = registry.lookup("UserController");
 *
 *   // 查找指定方法
 *   Optional<ControllerMethodMeta> method = registry.lookupMethod("UserController", "create");
 * }</pre>
 *
 * @see ControllerMeta
 * @see ControllerMethodMeta
 */
public final class LoggableControllerMetaRegistry implements SmartInitializingSingleton, Ordered {

  /**
   * JSON 元数据文件 classpath 搜索路径
   */
  private static final String META_RESOURCE_PATH = "META-INF/labzen/";

  /**
   * JSON 文件后缀
   */
  private static final String META_FILE_SUFFIX = ".meta.json";

  private final LabzenLogger logger = Loggers.getLogger(LoggableControllerMetaRegistry.class);
  /**
   * 所有 Controller 的元数据（只读）。
   */
  private Map<String, ControllerMeta> registry;

  @Resource
  private ObjectMapper objectMapper;

  /**
   * 启动时扫描 classpath 并加载所有 JSON 元数据文件。
   */
  @Override
  public void afterSingletonsInstantiated() {
    Map<String, ControllerMeta> loaded = loadAllMetaFiles();
    this.registry = loaded;
    logger.atInfo()
          .scene(LOGGER_SCENE_API_LOG_INIT)
          .status(Status.SUCCESS)
          .log("Controller元数据加载注册完成: 已处理 {} 个 Controller 的元数据", loaded.size());
  }

  @Override
  public int getOrder() {
    // 保证在 ApiLogConfigManager 之前执行，先加载好所有 Controller 的元数据
    return Integer.MIN_VALUE + 1_000;
  }

  // ============================================================
  // 公共查询方法
  // ============================================================

  /**
   * 获取所有 Controller 元数据的只读视图。
   *
   * @return Map&lt;Controller简单名, ControllerMeta&gt;
   */
  public Map<String, ControllerMeta> getAllMetas() {
    return registry;
  }

  /**
   * 按 Controller 简单名查找元数据。
   *
   * @param controllerSimpleName Controller 接口简单名（如 "UserController"）
   * @return ControllerMeta，未找到时返回空
   */
  public Optional<ControllerMeta> lookup(String controllerSimpleName) {
    return Optional.ofNullable(registry.get(controllerSimpleName));
  }

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
   * @return ControllerMethodMeta，未找到时返回空
   */
  public Optional<ControllerMethodMeta> lookupMethod(String controllerSimpleName, String methodKey) {
    return lookup(controllerSimpleName).flatMap(meta -> Optional.ofNullable(meta.methods().get(methodKey)));
  }

  // ============================================================
  // 加载逻辑
  // ============================================================

  /**
   * 扫描 classpath 下所有 .meta.json 元数据文件并加载。
   * <p>
   * 使用 Spring 的 {@code PathMatchingResourcePatternResolver}，
   * JAR 包和文件系统两种部署模式均适用。
   */
  private Map<String, ControllerMeta> loadAllMetaFiles() {
    Map<String, ControllerMeta> loaded = Maps.newHashMap();
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      var resources = resolver.getResources("classpath*:" + META_RESOURCE_PATH + "*" + META_FILE_SUFFIX);
      for (var resource : resources) {
        loadSingleResource(resource.getFilename(), resource.getInputStream(), loaded);
      }
      return Collections.unmodifiableMap(loaded);
    } catch (IOException e) {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_INIT)
            .status(Status.IMPORTANT)
            .setCause(e)
            .log("加载 Controller 元数据文件失败");
      return Collections.emptyMap();
    }
  }

  /**
   * 加载单个 JSON 元数据文件并解析为 ControllerMeta。
   * <p>
   * 每个 API 端点注册三个 key：
   * <ol>
   *   <li>hash key（方法签名的 MD5 前 5 位）</li>
   *   <li>方法名（如 "create"）</li>
   *   <li>HTTP方法+URL（如 "POST /api/user"）</li>
   * </ol>
   */
  private void loadSingleResource(String filename, InputStream inputStream, Map<String, ControllerMeta> loaded) {
    if (filename == null || !filename.endsWith(META_FILE_SUFFIX)) {
      return;
    }

    try (inputStream) {
      Map<String, Object> raw = objectMapper.readValue(inputStream, new TypeReference<>() {
      });

      String interfaceName = (String) raw.get("interfaceName");
      Class<?> interfaceClass = Class.forName(interfaceName);
      String simpleName = (String) raw.get("simpleName");

      Map<String, ControllerMethodMeta> methods = new LinkedHashMap<>();
      Object methodsObj = raw.get("methods");
      int loadedMethodCount = 0;
      if (methodsObj instanceof Map<?, ?> methodsMap) {
        for (Map.Entry<?, ?> entry : methodsMap.entrySet()) {
          String hashKey = (String) entry.getKey();  // MD5 前5位
          if (entry.getValue() instanceof Map<?, ?> methodData) {
            String methodName = (String) methodData.get("methodName");
            String httpMethod = (String) methodData.get("httpMethod");
            String urlPattern = (String) methodData.get("urlPattern");
            String fullUrlPattern = (String) methodData.get("fullUrlPattern");

            List<String> parameterTypes = new ArrayList<>();
            Object paramsObj = methodData.get("parameterTypes");
            if (paramsObj instanceof List<?> list) {
              for (Object p : list) {
                parameterTypes.add((String) p);
              }
            }

            ControllerMethodMeta meta = new ControllerMethodMeta(hashKey,
                methodName,
                httpMethod,
                urlPattern,
                fullUrlPattern,
                parameterTypes);

            // 注册三个 key：hash、方法名、HTTP方法+URL（自行组装）
            methods.putIfAbsent(hashKey, meta);
            if (methodName != null) {
              methods.putIfAbsent(methodName, meta);
            }
            String httpUrlKey = httpMethod + " " + fullUrlPattern;
            methods.putIfAbsent(httpUrlKey, meta);

            loadedMethodCount++;
          }
        }
      }

      ControllerMeta controller = new ControllerMeta(interfaceClass, simpleName, methods);
      loaded.put(simpleName, controller);
      logger.atDebug()
            .scene(LOGGER_SCENE_API_LOG_INIT)
            .status(Status.REMIND)
            .log("已加载 Controller 元数据: {} ({} 个端点)", simpleName, loadedMethodCount);
    } catch (IOException | ClassNotFoundException e) {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_INIT)
            .status(Status.IMPORTANT)
            .setCause(e)
            .log("解析元数据文件 [{}] 失败", filename);
    }
  }
}
