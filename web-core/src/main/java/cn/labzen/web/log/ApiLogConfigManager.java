package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.meta.Labzens;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.log.registry.ControllerMethodMeta;
import cn.labzen.web.log.bean.ApiEndpointDetail;
import cn.labzen.web.meta.WebCoreConfiguration;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.labzen.web.api.definition.Constants.*;

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
public final class ApiLogConfigManager implements SmartInitializingSingleton, Ordered, DisposableBean {

  private static final long CLEANUP_INTERVAL_SECONDS = 30;

  private final LabzenLogger logger = Loggers.getLogger(ApiLogConfigManager.class);

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
   * resolveConfig 结果缓存。
   * <p>
   * Key 格式: "ControllerName:methodKey"，与 runtimeConfigs 保持一致。
   * 运行时修改 API（enable/disable/setLevel/enableTemporarily/registerCondition/unregisterCondition/updateConfig）
   * 会失效对应的缓存条目。
   */
  private final Map<String, ApiEndpointLogConfig> resolvedConfigCache = new ConcurrentHashMap<>();

  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "labzen-web-log-config-cleanup");
    t.setDaemon(true);
    return t;
  });

  private final AtomicReference<ApiLogConfig> globalApiLogConfig = new AtomicReference<>();

  @Resource
  private LoggableControllerMetaRegistry registry;

  // ============================================================
  // 初始化
  // ============================================================

  /**
   * 在所有单例 Bean 初始化完成后执行，确保 {@link LoggableControllerMetaRegistry} 已加载元数据。
   */
  @Override
  public void afterSingletonsInstantiated() {
    startCleanupScheduler();
    initGlobalDefaults();
    loadConfigFormYaml();
  }

  @Override
  public int getOrder() {
    // 保证在 LoggableControllerMetaRegistry 之后执行，需要读取 Controller 的元数据
    return Integer.MIN_VALUE + 2_000;
  }

  private void initGlobalDefaults() {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    ApiLogConfig config = new ApiLogConfig();
    config.setEnabled(configuration.apiLogEnabled());
    config.setLevel(configuration.apiLogLevel());
    config.setLogRequest(configuration.apiLogRequest());
    config.setLogResponse(configuration.apiLogResponse());
    config.setSamplingRate(configuration.apiLogSamplingRate());

    globalApiLogConfig.set(config);
  }

  private void loadConfigFormYaml() {
    ApiLogConfigLoader loader = new ApiLogConfigLoader(registry);
    Map<String, Map<String, ApiLogConfig>> loaded = loader.loadAll();
    yamlConfigs.putAll(loaded);
    logger.atInfo()
          .scene(LOGGER_SCENE_API_LOG_INIT)
          .status(Status.SUCCESS)
          .log("API 日志配置初始化完成: 已加载 {} 个 YAML 配置", loaded.size());
  }

  private void startCleanupScheduler() {
    cleanupExecutor.scheduleWithFixedDelay(() -> {
      try {
        programmaticConfigs.forEach((controllerName, methodConfigs) -> {
          Set<Map.Entry<String, ApiEndpointLogConfig>> entries = methodConfigs.entrySet();
          entries.removeIf(entry -> {
            ApiEndpointLogConfig config = entry.getValue();
            return config.getExpiresAt() != null && Instant.now().isAfter(config.getExpiresAt());
          });
        });
      } catch (Exception e) {
        logger.atWarn()
              .scene(LOGGER_SCENE_API_LOG_CONFIG)
              .status(Status.FAILED)
              .setCause(e)
              .log("API 日志清理过期配置发生问题");
      }
    }, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  // ============================================================
  // 程序化配置 API
  // ============================================================

  /**
   * 动态全局配置
   */
  @SuppressWarnings("unused")
  public void configureGlobal(ApiLogConfig config) {
    globalApiLogConfig.get().merge(config);
    logger.atWarn()
          .scene(LOGGER_SCENE_API_LOG_CONFIG)
          .status(Status.SUCCESS)
          .log("API 日志输出全局配置已更新: {}", config);
  }

  /**
   * 动态 Controller 方法配置
   */
  @SuppressWarnings("unused")
  public void configureMethod(String controllerName, String methodHash, ApiEndpointLogConfig config) {
    Optional<ControllerMeta> lookup = registry.lookup(controllerName);
    if (lookup.isPresent()) {
      ControllerMeta controllerMeta = lookup.get();
      ControllerMethodMeta controllerMethodMeta = controllerMeta.methods().get(methodHash);
      if (controllerMethodMeta != null) {
        Map<String, ApiEndpointLogConfig> methodConfigs = programmaticConfigs.computeIfAbsent(controllerName,
            k -> new ConcurrentHashMap<>());
        if (config == null) {
          methodConfigs.remove(methodHash);
          logger.atInfo()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.SUCCESS)
                .log("API 日志方法级配置已删除: {}, method={}, url={}",
                    controllerName,
                    controllerMethodMeta.methodName(),
                    controllerMethodMeta.fullUrlPattern());
        } else {
          methodConfigs.put(methodHash, config);
          logger.atInfo()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.SUCCESS)
                .log("API 日志方法级配置已更新: {}, method={}, url={}, level={}",
                    controllerName,
                    controllerMethodMeta.methodName(),
                    controllerMethodMeta.fullUrlPattern(),
                    config);
        }
        resolvedConfigCache.remove(controllerName + ":" + methodHash);
      } else {
        logger.atWarn()
              .scene(LOGGER_SCENE_API_LOG_CONFIG)
              .status(Status.FIXME)
              .log("API 日志方法级配置更新失败: 未找到对应方法: {}, method={}", controllerName, methodHash);
      }
    } else {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.FIXME)
            .log("API 日志方法级配置更新失败: 未找到对应 Controller: {}", controllerName);
    }
  }

  // ============================================================
  // 运行时配置查询（唯一入口）
  // ============================================================

  /**
   * 按 Controller 名和方法标识解析最终生效的配置（三层合并 + 缓存）。
   * <p>
   * 合并顺序：框架默认 → classpath YAML → 程序化 API。
   *
   * @param controllerName Controller 接口名
   * @param methodHash     方法标识（方法哈希）
   * @return 最终生效的配置（包含可能的 conditionGroup）
   */
  public ApiEndpointLogConfig resolveConfig(String controllerName, String methodHash) {
    String cacheKey = controllerName + ":" + methodHash;
    return resolvedConfigCache.computeIfAbsent(cacheKey, k -> doResolve(controllerName, methodHash));
  }

  private ApiEndpointLogConfig doResolve(String controllerName, String methodHash) {
    ApiEndpointLogConfig resolved = new ApiEndpointLogConfig();

    // step 1. 先合并全局配置
    resolved.merge(globalApiLogConfig.get());

    Map<String, ApiLogConfig> yamlController = yamlConfigs.get(controllerName);
    if (yamlController != null) {
      // step 2. 再合并YAML定义的 controller 通用配置
      ApiLogConfig yamlGeneral = yamlController.get(API_LOG_KEY_GENERAL);
      if (yamlGeneral != null) {
        resolved.merge(yamlGeneral);
      }

      // step 3. 再合并YAML定义的方法配置
      ApiLogConfig yamlMethod = yamlController.get(methodHash);
      if (yamlMethod != null) {
        resolved.merge(yamlMethod);
      }
    }

    Map<String, ApiEndpointLogConfig> progController = programmaticConfigs.get(controllerName);
    if (progController != null) {
      ApiEndpointLogConfig progMethod = progController.get(methodHash);
      if (progMethod != null) {
        resolved.merge(progMethod);
      }
    }

    return resolved;
  }

  // ============================================================
  // API 端点详情查询（供业务项目 LogManagementController 使用）
  // ============================================================

  /**
   * 获取所有 Controller 名列表。
   */
  @SuppressWarnings("unused")
  public List<String> allControllerNames() {
    return registry.getAllMetas().values().stream().map(ControllerMeta::interfaceClass).map(Class::getName).toList();
  }

  /**
   * 获取 Controller 的所有方法详情。
   */
  @SuppressWarnings("unused")
  public List<ApiEndpointDetail> getApiEndpointsDetail(String controllerName) {
    if (registry == null) {
      return Collections.emptyList();
    }

    return registry.lookup(controllerName).map(meta -> {
      List<ApiEndpointDetail> result = new ArrayList<>();
      for (Map.Entry<String, ControllerMethodMeta> entry : meta.methods().entrySet()) {
        String methodKey = entry.getKey();
        ControllerMethodMeta methodMeta = entry.getValue();

        if (!methodKey.equals(methodMeta.methodName())) {
          continue;
        }

        ApiEndpointLogConfig config = resolveConfig(controllerName, methodKey);
        result.add(new ApiEndpointDetail(methodMeta.httpMethod(),
            methodMeta.fullUrlPattern(),
            methodMeta.methodName(),
            controllerName,
            methodMeta.parameterTypes(),
            config));
      }
      return result;
    }).orElse(Collections.emptyList());
  }

  @Override
  public void destroy() {
    cleanupExecutor.shutdown();
  }

}
