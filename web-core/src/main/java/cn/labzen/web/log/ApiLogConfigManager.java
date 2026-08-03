package cn.labzen.web.log;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.log.registry.ControllerMethodMeta;
import lombok.extern.slf4j.Slf4j;

import static cn.labzen.web.api.definition.Constants.API_LOG_KEY_GENERAL;
import static cn.labzen.web.api.definition.Constants.API_LOG_KEY_GLOBAL;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * API 日志配置统一管理组件。
 * <p>
 * 一个端点（Controller + 方法）只有一个配置，通过 {@link #resolveConfig(String, String)} 获取。
 * 配置来源按优先级合并：
 * <ol>
 *   <li>运行时动态修改（registerCondition / enable / enableTemporarily / updateConfig）</li>
 *   <li>启动时程序化 API（configureGlobalDefaults / configureController / configureMethod）</li>
 *   <li>classpath YAML（resources/labzen-web/{ControllerName}.yml）</li>
 *   <li>框架默认值（enabled=false, level=DEBUG, samplingRate=1.0）</li>
 * </ol>
 * <p>
 * <b>线程安全设计：</b>
 * <ul>
 *   <li>yamlConfigs / programmaticConfigs / runtimeConfigs：{@link ConcurrentHashMap}</li>
 *   <li>过期清理：{@link ScheduledExecutorService} 单线程</li>
 * </ul>
 *
 * @see ApiEndpointLogConfig
 * @see ApiLogConfigLoader
 * @see ApiLogConditionEvaluator
 */
@Slf4j
public final class ApiLogConfigManager {

  private static final long CLEANUP_INTERVAL_SECONDS = 30;

  // ============================================================
  // 存储结构
  // ============================================================

  /**
   * classpath YAML 配置缓存（启动时一次性加载）
   */
  private final Map<String, Map<String, ApiLogConfig>> yamlConfigs = new ConcurrentHashMap<>();

  /**
   * 启动时程序化配置
   */
  private final Map<String, Map<String, ApiEndpointLogConfig>> programmaticConfigs = new ConcurrentHashMap<>();

  /**
   * 运行时动态修改配置（仅内存，重启丢失）。
   * <p>
   * Key 格式: "ControllerName:methodKey"，其中 methodKey 可以是方法哈希、方法名或 HTTP方法+URL。
   * 所有运行时修改（enable/disable/enableTemporarily/registerCondition/updateConfig）都写入此 Map。
   */
  private final Map<String, ApiEndpointLogConfig> runtimeConfigs = new ConcurrentHashMap<>();

  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "labzen-web-log-config-cleanup");
    t.setDaemon(true);
    return t;
  });

  private final Runnable cleanupTask = () -> {
    try {
      runtimeConfigs.entrySet().removeIf(entry -> {
        ApiEndpointLogConfig config = entry.getValue();
        return config.getExpiresAt() != null && Instant.now().isAfter(config.getExpiresAt());
      });
    } catch (Exception e) {
      logger.warn("API 日志过期配置清理失败", e);
    }
  };

  private final LoggableControllerMetaRegistry registry;

  // ============================================================
  // 初始化
  // ============================================================

  public ApiLogConfigManager(LoggableControllerMetaRegistry registry) {
    this.registry = registry;
    loadYamlConfigs();
    startCleanupScheduler();
  }

  private void loadYamlConfigs() {
    ApiLogConfigLoader loader = new ApiLogConfigLoader();
    Map<String, Map<String, ApiLogConfig>> loaded = loader.loadAll(registry);
    yamlConfigs.putAll(loaded);
    logger.info("API 日志配置管理器初始化完成: 已加载 {} 个 Controller 的 YAML 配置", loaded.size());
  }

  private void startCleanupScheduler() {
    cleanupExecutor.scheduleWithFixedDelay(cleanupTask, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  // ============================================================
  // 启动时程序化配置 API（最高优先级）
  // ============================================================

  public void configureGlobalDefaults(ApiEndpointLogConfig config) {
    programmaticConfigs.computeIfAbsent(API_LOG_KEY_GLOBAL, k -> new ConcurrentHashMap<>())
      .put(API_LOG_KEY_GENERAL, config);
    logger.info("API 日志全局默认配置已更新: level={}, enabled={}", config.getLevel(), config.isEnabled());
  }

  public void configureController(String controllerName, ApiEndpointLogConfig config) {
    programmaticConfigs.computeIfAbsent(controllerName, k -> new ConcurrentHashMap<>())
      .put(API_LOG_KEY_GENERAL, config);
    logger.info("API 日志 Controller 级配置已更新: controller={}, level={}", controllerName, config.getLevel());
  }

  public void configureMethod(String controllerName, String methodKey, ApiEndpointLogConfig config) {
    programmaticConfigs.computeIfAbsent(controllerName, k -> new ConcurrentHashMap<>())
      .put(methodKey, config);
    logger.info("API 日志方法级配置已更新: controller={}, method={}, level={}", controllerName, methodKey, config.getLevel());
  }

  // ============================================================
  // 运行时配置查询（唯一入口）
  // ============================================================

  /**
   * 按 Controller 名和方法标识解析最终生效的配置（三层合并）。
   * <p>
   * 合并顺序：框架默认 → classpath YAML → 程序化 API → 运行时覆盖。
   * 运行时覆盖包括 enable/disable/enableTemporarily/registerCondition/updateConfig 等所有动态修改。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识（方法哈希、方法名 或 HTTP方法+URL）
   * @return 最终生效的配置（包含可能的 conditionGroup）
   */
  public ApiEndpointLogConfig resolveConfig(String controllerName, String methodKey) {
    ApiEndpointLogConfig resolved = new ApiEndpointLogConfig();

    // classpath YAML
    Map<String, ApiLogConfig> yamlController = yamlConfigs.get(controllerName);
    if (yamlController != null) {
      ApiLogConfig yamlGeneral = yamlController.get(API_LOG_KEY_GENERAL);
      if (yamlGeneral != null) {
        resolved = resolved.mergeFrom(yamlGeneral);
      }
      ApiLogConfig yamlMethod = yamlController.get(methodKey);
      if (yamlMethod != null) {
        resolved = resolved.mergeFrom(yamlMethod);
      }
    }

    // 程序化 — 全局
    Map<String, ApiEndpointLogConfig> globalProg = programmaticConfigs.get(API_LOG_KEY_GLOBAL);
    if (globalProg != null) {
      ApiLogConfig progGlobal = globalProg.get(API_LOG_KEY_GENERAL);
      if (progGlobal != null) {
        resolved = resolved.mergeFrom(progGlobal);
      }
    }

    // 程序化 — Controller + 方法级
    Map<String, ApiEndpointLogConfig> progController = programmaticConfigs.get(controllerName);
    if (progController != null) {
      ApiLogConfig progGeneral = progController.get(API_LOG_KEY_GENERAL);
      if (progGeneral != null) {
        resolved = resolved.mergeFrom(progGeneral);
      }
      ApiEndpointLogConfig progMethod = progController.get(methodKey);
      if (progMethod != null) {
        resolved = resolved.mergeFrom(progMethod);
      }
    }

    // 运行时覆盖（最高优先级，包含条件配置）
    String runtimeKey = controllerName + ":" + methodKey;
    ApiEndpointLogConfig runtimeConfig = runtimeConfigs.get(runtimeKey);
    if (runtimeConfig != null) {
      resolved = resolved.mergeFrom(runtimeConfig);
    }

    return resolved;
  }

  // ============================================================
  // 运行时动态修改 API（仅内存，重启丢失）
  // ============================================================

  public void enable(String controllerName, String methodKey) {
    String key = controllerName + ":" + methodKey;
    ApiEndpointLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiEndpointLogConfig());
    config.setEnabled(true);
    logger.info("API 日志已启用: {}.{}", controllerName, methodKey);
  }

  public void disable(String controllerName, String methodKey) {
    String key = controllerName + ":" + methodKey;
    ApiEndpointLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiEndpointLogConfig());
    config.setEnabled(false);
    logger.info("API 日志已禁用: {}.{}", controllerName, methodKey);
  }

  public void setLevel(String controllerName, String methodKey, String level) {
    setLevel(controllerName, methodKey, org.slf4j.event.Level.valueOf(level));
  }

  public void setLevel(String controllerName, String methodKey, org.slf4j.event.Level level) {
    String key = controllerName + ":" + methodKey;
    ApiEndpointLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiEndpointLogConfig());
    config.setLevel(level);
    logger.info("API 日志级别已更新: {}.{} → {}", controllerName, methodKey, level);
  }

  public void enableTemporarily(String controllerName, String methodKey, Duration duration) {
    String key = controllerName + ":" + methodKey;
    ApiEndpointLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiEndpointLogConfig());
    config.setEnabled(true);
    config.setExpiresAt(Instant.now().plus(duration));
    logger.info("API 日志临时开启: {}.{}，将在 {} 后自动关闭", controllerName, methodKey, duration);
  }

  public void updateConfig(String controllerName, String methodKey, ApiEndpointLogConfig config) {
    String key = controllerName + ":" + methodKey;
    runtimeConfigs.put(key, config);
    logger.info("API 日志配置已更新: {}.{}", controllerName, methodKey);
  }

  /**
   * 为指定端点注册条件配置。
   * <p>
   * 条件配置通过 {@link #resolveConfig} 的三层合并返回，
   * 拦截器拿到配置后检查 {@code isConditional()} 决定是否评估条件。
   *
   * @param controllerName Controller 接口名
   * @param methodHash     方法签名哈希（5位 hex）
   * @param config         条件配置（isConditional() 必须为 true）
   */
  public void registerCondition(String controllerName, String methodHash, ApiEndpointLogConfig config) {
    String key = controllerName + ":" + methodHash;
    runtimeConfigs.put(key, config);
    logger.info("条件日志已注册: endpoint={}, conditionGroup={}, ttl={}",
      key, config.getConditionGroup(), config.getTtl());
  }

  /**
   * 注销指定端点的条件配置。
   */
  public boolean unregisterCondition(String controllerName, String methodHash) {
    String key = controllerName + ":" + methodHash;
    ApiEndpointLogConfig removed = runtimeConfigs.remove(key);
    if (removed != null) {
      logger.info("条件日志已注销: endpoint={}", key);
      return true;
    }
    return false;
  }

  // ============================================================
  // API 端点详情查询（供业务项目 LogManagementController 使用）
  // ============================================================

  public List<ApiEndpointDetail> getApiEndpointsDetail() {
    if (registry == null) {
      return Collections.emptyList();
    }

    List<ApiEndpointDetail> result = new ArrayList<>();

    for (Map.Entry<String, ControllerMeta> entry : registry.getAllMetas().entrySet()) {
      String controllerName = entry.getKey();
      ControllerMeta meta = entry.getValue();

      for (Map.Entry<String, ControllerMethodMeta> methodEntry : meta.methods().entrySet()) {
        String methodKey = methodEntry.getKey();
        ControllerMethodMeta methodMeta = methodEntry.getValue();

        if (!methodKey.equals(methodMeta.methodName())) {
          continue;
        }

        ApiEndpointLogConfig config = resolveConfig(controllerName, methodKey);

        result.add(new ApiEndpointDetail(
          methodMeta.httpMethod(),
          methodMeta.fullUrlPattern(),
          methodMeta.methodName(),
          controllerName,
          methodMeta.parameterTypes(),
          config
        ));
      }
    }

    result.sort(Comparator.comparing(ApiEndpointDetail::controllerName)
      .thenComparing(ApiEndpointDetail::httpMethod)
      .thenComparing(ApiEndpointDetail::urlPattern));

    return result;
  }

  public List<ApiEndpointDetail> getApiEndpointsDetail(String controllerName) {
    if (registry == null) {
      return Collections.emptyList();
    }

    return registry.lookup(controllerName)
      .map(meta -> {
        List<ApiEndpointDetail> result = new ArrayList<>();
        for (Map.Entry<String, ControllerMethodMeta> entry : meta.methods().entrySet()) {
          String methodKey = entry.getKey();
          ControllerMethodMeta methodMeta = entry.getValue();

          if (!methodKey.equals(methodMeta.methodName())) {
            continue;
          }

          ApiEndpointLogConfig config = resolveConfig(controllerName, methodKey);
          result.add(new ApiEndpointDetail(
            methodMeta.httpMethod(),
            methodMeta.fullUrlPattern(),
            methodMeta.methodName(),
            controllerName,
            methodMeta.parameterTypes(),
            config
          ));
        }
        return result;
      })
      .orElse(Collections.emptyList());
  }

  public ApiEndpointLogConfig getEndpointLogConfig(String controllerName, String methodKey) {
    return resolveConfig(controllerName, methodKey);
  }

  public void destroy() {
    cleanupExecutor.shutdown();
    try {
      if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public record ApiEndpointDetail(
    String httpMethod,
    String urlPattern,
    String methodName,
    String controllerName,
    List<String> parameterTypes,
    ApiEndpointLogConfig logConfig
  ) {
  }
}
