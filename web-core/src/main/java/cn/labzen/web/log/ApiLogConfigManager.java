package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import cn.labzen.web.api.log.config.ConditionGroup;
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
@SuppressWarnings("unused")
public final class ApiLogConfigManager implements SmartInitializingSingleton, Ordered, DisposableBean {

  //private static final long CLEANUP_INTERVAL_SECONDS = 30;

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
  private final Map<String, Map<String, ApiEndpointLogConfig>> programmaticEndpointConfigs = new ConcurrentHashMap<>();

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
  /**
   * Controller 级程序化配置（接口名 → 配置）。
   */
  private final Map<String, ApiLogConfig> programmaticControllerConfigs = new ConcurrentHashMap<>();

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
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    int interval = configuration.apiLogProgrammaticConfigUpdateInterval();
    cleanupExecutor.scheduleWithFixedDelay(() -> {
      try {
        programmaticEndpointConfigs.forEach((interfaceName, methodConfigs) -> {
          methodConfigs.forEach((methodKey, config) -> {
            if (!Boolean.TRUE.equals(config.getEnabled())) {
              return;
            }
            if (config.getExpiresAt() != null && Instant.now().isAfter(config.getExpiresAt())) {
              config.setEnabled(false);
              String cacheKey = endpointConfigCacheKey(interfaceName, methodKey);
              resolvedConfigCache.remove(cacheKey);
            }
          });
        });
      } catch (Exception e) {
        logger.atWarn()
              .scene(LOGGER_SCENE_API_LOG_CONFIG)
              .status(Status.FAILED)
              .setCause(e)
              .log("API 日志清理过期配置发生问题");
      }
    }, interval, interval, TimeUnit.SECONDS);
  }

  // ============================================================
  // 程序化配置 API
  // ============================================================

  /**
   * 获取当前全局日志配置。
   */
  public ApiLogConfig getGlobalConfig() {
    return globalApiLogConfig.get();
  }

  /**
   * 动态全局配置
   */
  public void configureGlobal(ApiLogConfig config) {
    globalApiLogConfig.get().merge(config);
    resolvedConfigCache.clear();
    logger.atWarn()
          .scene(LOGGER_SCENE_API_LOG_CONFIG)
          .status(Status.SUCCESS)
          .log("API 日志输出全局配置已更新: {}", config);
  }

  /**
   * 获取指定 Controller 的通用程序化配置。
   */
  @SuppressWarnings("DuplicatedCode")
  public ApiLogConfig getControllerConfig(String interfaceName) {
    ApiLogConfig resolved = new ApiLogConfig();
    resolved.merge(globalApiLogConfig.get());
    Map<String, ApiLogConfig> yamlConfig = yamlConfigs.get(interfaceName);
    if (yamlConfig != null) {
      ApiLogConfig yamlGeneral = yamlConfig.get(API_LOG_KEY_GENERAL);
      if (yamlGeneral != null) {
        resolved.merge(yamlGeneral);
      }
    }
    ApiLogConfig programmaticConfig = programmaticControllerConfigs.get(interfaceName);
    if (programmaticConfig != null) {
      resolved.merge(programmaticConfig);
    }
    return resolved;
  }

  /**
   * 动态 Controller 级通用配置（对该 Controller 的所有方法生效）。
   *
   * @param interfaceName Controller 全限定类名
   * @param config        配置对象
   */
  public void configureController(String interfaceName, ApiLogConfig config) {
    Optional<ControllerMeta> lookup = registry.lookup(interfaceName);
    if (lookup.isPresent()) {
      programmaticControllerConfigs.put(interfaceName, config);
      logger.atInfo()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.SUCCESS)
            .log("API 日志 Controller 级配置已更新: {}, {}", interfaceName, config);
      // 清除该 Controller 下所有方法的缓存
      resolvedConfigCache.keySet().removeIf(key -> key.startsWith(interfaceName + ":"));
    } else {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.FIXME)
            .log("API 日志 Controller 级配置更新失败: 未找到对应 Controller: {}", interfaceName);
    }
  }

  /**
   * 动态 Controller 方法配置
   */
  public void configureMethod(String interfaceName, String methodHash, ApiEndpointLogConfig config) {
    Optional<ControllerMeta> lookup = registry.lookup(interfaceName);
    if (lookup.isEmpty()) {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.FIXME)
            .log("API 日志方法级配置更新失败: 未找到对应 Controller: {}", interfaceName);
      return;
    }

    ControllerMeta controllerMeta = lookup.get();
    if (!controllerMeta.methods().containsKey(methodHash)) {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.FIXME)
            .log("API 日志方法级配置更新失败: 未找到对应方法: {}, method={}", interfaceName, methodHash);
      return;
    }

    ControllerMethodMeta controllerMethodMeta = controllerMeta.methods().get(methodHash);
    Map<String, ApiEndpointLogConfig> methodConfigs = programmaticEndpointConfigs.computeIfAbsent(interfaceName,
        k -> new ConcurrentHashMap<>());
    if (config == null) {
      methodConfigs.remove(methodHash);
      logger.atInfo()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.SUCCESS)
            .log("API 日志方法级配置已删除: {}, method={}, url={}",
                interfaceName,
                controllerMethodMeta.methodName(),
                controllerMethodMeta.fullUrlPattern());
    } else {
      config.setCreatedAt(Instant.now());

      // 条件表达式：解析并验证 TTL
      if (Strings.isNotBlank(config.getConditionExpression())) {
        if (config.getTtl() == null) {
          logger.atWarn()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.FIXME)
                .log("API 日志方法级配置更新失败: conditionExpression 必须同时设置 ttl: {}, method={}",
                    interfaceName,
                    controllerMethodMeta.methodName());
          return;
        }

        ConditionGroup group = ConditionExpressionParser.parse(config.getConditionExpression());
        if (group != null) {
          config.setConditionGroup(group);
        } else {
          logger.atWarn()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.FIXME)
                .log("API 日志方法级配置更新失败: 解析 conditionExpression 失败，将忽略条件表达式: {}, method={}",
                    interfaceName,
                    controllerMethodMeta.methodName());
          return;
        }
      }

      if (config.getTtl() != null && !config.getTtl().isZero()) {
        config.setExpiresAt(Instant.now().plus(config.getTtl()));
      }

      methodConfigs.put(methodHash, config);
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.SUCCESS)
            .log("API 日志方法级配置已更新: {}, method={}, url={}, level={}",
                interfaceName,
                controllerMethodMeta.methodName(),
                controllerMethodMeta.fullUrlPattern(),
                config);
    }

    String cacheKey = endpointConfigCacheKey(interfaceName, methodHash);
    resolvedConfigCache.remove(cacheKey);
  }

  // ============================================================
  // 运行时配置查询（唯一入口）
  // ============================================================

  private String endpointConfigCacheKey(String interfaceName, String methodHash) {
    return interfaceName + ":" + methodHash;
  }

  public ApiEndpointLogConfig resolveConfigWithoutCache(String interfaceName, String methodHash) {
    return doResolve(interfaceName, methodHash);
  }

  /**
   * 按 Controller 名和方法标识解析最终生效的配置（三层合并 + 缓存）。
   * <p>
   * 合并顺序：框架默认 → classpath YAML → 程序化 API。
   *
   * @param interfaceName Controller 接口名
   * @param methodHash    方法标识（方法哈希）
   * @return 最终生效的配置（包含可能的 conditionGroup）
   */
  public ApiEndpointLogConfig resolveConfig(String interfaceName, String methodHash) {
    String cacheKey = endpointConfigCacheKey(interfaceName, methodHash);
    return resolvedConfigCache.computeIfAbsent(cacheKey, k -> doResolve(interfaceName, methodHash));
  }

  @SuppressWarnings("DuplicatedCode")
  private ApiEndpointLogConfig doResolve(String interfaceName, String methodHash) {
    ApiEndpointLogConfig resolved = new ApiEndpointLogConfig();

    // step 1. 先合并全局配置
    resolved.merge(globalApiLogConfig.get());

    Map<String, ApiLogConfig> yamlControllerConfigs = this.yamlConfigs.get(interfaceName);
    if (yamlControllerConfigs != null) {
      // step 2. 再合并YAML定义的 controller 通用配置
      ApiLogConfig yamlGeneralConfig = yamlControllerConfigs.get(API_LOG_KEY_GENERAL);
      if (yamlGeneralConfig != null) {
        resolved.merge(yamlGeneralConfig);
      }
    }

    // step 3. 合并程序化 Controller 级配置
    ApiLogConfig programmaticControllerConfig = programmaticControllerConfigs.get(interfaceName);
    if (programmaticControllerConfig != null) {
      resolved.merge(programmaticControllerConfig);
    }

    if (yamlControllerConfigs != null) {
      // step 4. 再合并YAML定义的方法配置
      ApiLogConfig yamlEndpointConfig = yamlControllerConfigs.get(methodHash);
      if (yamlEndpointConfig != null) {
        resolved.merge(yamlEndpointConfig);
      }
    }

    // step 5. 合并程序化方法级配置
    Map<String, ApiEndpointLogConfig> programmaticEndpointConfigs = this.programmaticEndpointConfigs.get(interfaceName);
    if (programmaticEndpointConfigs != null) {
      ApiEndpointLogConfig programmaticEndpointConfig = programmaticEndpointConfigs.get(methodHash);
      if (programmaticEndpointConfig != null) {
        resolved.merge(programmaticEndpointConfig);
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
  public List<String> allControllerNames() {
    return registry.getAllMetas().values().stream().map(ControllerMeta::interfaceClass).map(Class::getName).toList();
  }

  /**
   * 获取 Controller 的所有方法详情。
   */
  public List<ApiEndpointDetail> getApiEndpointsDetail(String interfaceName) {
    if (registry == null) {
      return Collections.emptyList();
    }

    return registry.lookup(interfaceName).map(meta -> {
      List<ApiEndpointDetail> result = new ArrayList<>();
      for (Map.Entry<String, ControllerMethodMeta> entry : meta.methods().entrySet()) {
        String methodKey = entry.getKey();
        ControllerMethodMeta methodMeta = entry.getValue();

        if (!methodKey.equals(methodMeta.hash())) {
          continue;
        }

        ApiEndpointLogConfig config = resolveConfigWithoutCache(interfaceName, methodKey);
        result.add(new ApiEndpointDetail(methodMeta.httpMethod(),
            methodMeta.fullUrlPattern(),
            methodMeta.methodName(),
            methodMeta.hash(),
            interfaceName,
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
