package cn.labzen.web.log;

import cn.labzen.web.api.log.ApiLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.log.registry.ControllerMethodMeta;
import cn.labzen.web.api.log.registry.LoggableControllerMetaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * API 日志配置统一管理组件。
 * <p>
 * 由 {@code LabzenWebConfigurer} 创建并注册为 Spring Bean，负责：
 * <ul>
 *   <li><b>启动时初始化</b>：加载 classpath YAML 配置，提供 {@link #configureGlobalDefaults(ApiLogConfig)}、
 *       {@link #configureController(String, ApiLogConfig)}、{@link #configureMethod(String, String, ApiLogConfig)}
 *       等程序化配置 API</li>
 *   <li><b>运行时配置查询</b>：{@link #resolveConfig(String, String)} 按三层合并返回最终生效配置</li>
 *   <li><b>运行时动态修改</b>：{@link #enable(String, String)}、{@link #setLevel(String, String, String)}、
 *       {@link #updateConfig(String, String, ApiLogConfig)} 等，修改仅存内存，重启后丢失</li>
 *   <li><b>临时开启</b>：{@link #enableTemporarily(String, String, Duration)} 针对指定接口方法无条件临时开启，到期自动关闭</li>
 *   <li><b>条件日志</b>：通过 {@link #registerCondition(ApiLogConfig)} 注册条件触发配置（matchType 非 null），到期自动清理</li>
 *   <li><b>过期自动清理</b>：后台调度线程每 30 秒扫描并移除过期条件和临时开启配置</li>
 * </ul>
 * <p>
 * <b>配置三层合并（优先级从高到低）：</b>
 * <ol>
 *   <li>启动时程序化 API 配置（通过 {@code configureXxx} 方法设置的）</li>
 *   <li>classpath YAML 配置（{@code resources/labzen-web/{ControllerName}.yml}）</li>
 *   <li>框架默认值（{@code enabled=false, level=DEBUG, samplingRate=1.0}）</li>
 * </ol>
 * <p>
 * <b>线程安全设计：</b>
 * <ul>
 *   <li>启动时程序化配置：{@link ConcurrentHashMap}</li>
 *   <li>运行时动态配置：{@link ConcurrentHashMap}</li>
 *   <li>条件列表：{@link CopyOnWriteArrayList}</li>
 *   <li>过期清理：{@link ScheduledExecutorService} 单线程</li>
 * </ul>
 *
 * @see ApiLogConfig
 * @see YamlApiLogStore
 * @see ApiLogConditionEvaluator
 */
public class ApiLogConfigManager {

  private static final Logger log = LoggerFactory.getLogger(ApiLogConfigManager.class);

  /**
   * 过期条件清理间隔（秒）
   */
  private static final long CLEANUP_INTERVAL_SECONDS = 30;

  // ============================================================
  // 存储结构
  // ============================================================

  /**
   * classpath YAML 配置缓存（启动时一次性加载）
   * <p>
   * 外层 Key: Controller 接口名；内层 Key: 方法标识（"__general__" 为通用配置）
   */
  private final Map<String, Map<String, ApiLogConfig>> yamlConfigs = new ConcurrentHashMap<>();

  /**
   * 启动时程序化配置（最高优先级）
   * <p>
   * 结构同 yamlConfigs
   */
  private final Map<String, Map<String, ApiLogConfig>> programmaticConfigs = new ConcurrentHashMap<>();

  /**
   * 运行时动态修改配置（仅内存，重启丢失）
   * <p>
   * Key 格式: "ControllerName:methodKey"
   */
  private final Map<String, ApiLogConfig> runtimeConfigs = new ConcurrentHashMap<>();

  /**
   * 活跃的条件触发配置列表（线程安全）。
   * <p>
   * 仅存储 {@code matchType != null} 的条件配置，用于拦截器中按请求参数匹配。
   */
  private final List<ApiLogConfig> activeConditionalConfigs = new CopyOnWriteArrayList<>();

  /**
   * Controller 元数据注册表（由 LabzenWebConfigurer 注入）。
   * <p>
   * 用于 {@link #getApiEndpointsDetail()} 提供所有 API 端点的元数据信息。
   */
  private LoggableControllerMetaRegistry registry;

  /**
   * 过期清理调度器
   */
  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "labzen-api-log-cleanup");
    t.setDaemon(true);
    return t;
  });

  // ============================================================
  // 初始化
  // ============================================================

  /**
   * 构造函数：加载 classpath YAML 配置并启动过期清理调度器。
   */
  public ApiLogConfigManager() {
    loadYamlConfigs();
    startCleanupScheduler();
  }

  /**
   * 从 classpath 加载 YAML 配置。
   */
  private void loadYamlConfigs() {
    YamlApiLogStore store = new YamlApiLogStore();
    Map<String, Map<String, ApiLogConfig>> loaded = store.loadAll();
    yamlConfigs.putAll(loaded);
    log.info("API 日志配置管理器初始化完成: 已加载 {} 个 Controller 的 YAML 配置", loaded.size());
  }

  /**
   * 启动过期条件/配置清理调度器。
   * <p>
   * 每 30 秒扫描一次，移除已过期的条件配置和临时开启配置。
   */
  private void startCleanupScheduler() {
    cleanupExecutor.scheduleWithFixedDelay(() -> {
      try {
        // 清理过期条件配置
        int before = activeConditionalConfigs.size();
        activeConditionalConfigs.removeIf(ApiLogConfig::isExpired);
        int after = activeConditionalConfigs.size();
        if (before != after) {
          log.debug("API 日志过期条件已清理: {} → {} (移除了 {} 条)", before, after, before - after);
        }

        // 清理过期的临时开启配置
        runtimeConfigs.entrySet().removeIf(entry -> {
          ApiLogConfig config = entry.getValue();
          return config.getExpiresAt() != null && Instant.now().isAfter(config.getExpiresAt());
        });
      } catch (Exception e) {
        log.warn("API 日志过期条件清理失败", e);
      }
    }, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  // ============================================================
  // 启动时程序化配置 API（最高优先级）
  // ============================================================

  /**
   * 修改全局默认配置（影响所有未单独配置的 Controller 和方法）。
   * <p>
   * 此方法设置的配置优先级最高，覆盖 classpath YAML 和框架默认值。
   * 典型使用场景：在业务项目的 {@code @Configuration} 或 {@code ApplicationRunner} 中调用。
   *
   * <pre>{@code
   *   @Configuration
   *   public class ApiLogConfiguration {
   *       @Autowired
   *       public void initApiLog(ApiLogConfigManager manager) {
   *           ApiLogConfig config = new ApiLogConfig();
   *           config.setLevel("INFO");
   *           config.setSamplingRate(0.3);
   *           manager.configureGlobalDefaults(config);
   *       }
   *   }
   * }</pre>
   *
   * @param config 全局默认配置
   */
  public void configureGlobalDefaults(ApiLogConfig config) {
    programmaticConfigs.computeIfAbsent("__global__", k -> new ConcurrentHashMap<>())
      .put("__general__", config);
    log.info("API 日志全局默认配置已更新: level={}, enabled={}", config.getLevel(), config.isEnabled());
  }

  /**
   * 修改指定 Controller 的默认配置（对该 Controller 下所有未单独配置的方法生效）。
   *
   * @param controllerName Controller 接口名（如 "UserController"）
   * @param config         配置
   */
  public void configureController(String controllerName, ApiLogConfig config) {
    programmaticConfigs.computeIfAbsent(controllerName, k -> new ConcurrentHashMap<>())
      .put("__general__", config);
    log.info("API 日志 Controller 级配置已更新: controller={}, level={}", controllerName, config.getLevel());
  }

  /**
   * 修改指定方法的配置。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识（方法名 或 HTTP方法+URL，如 "create" 或 "POST /api/user"）
   * @param config         配置
   */
  public void configureMethod(String controllerName, String methodKey, ApiLogConfig config) {
    programmaticConfigs.computeIfAbsent(controllerName, k -> new ConcurrentHashMap<>())
      .put(methodKey, config);
    log.info("API 日志方法级配置已更新: controller={}, method={}, level={}", controllerName, methodKey, config.getLevel());
  }

  // ============================================================
  // 运行时配置查询
  // ============================================================

  /**
   * 按 Controller 名和方法标识解析最终生效的配置（三层合并）。
   * <p>
   * 合并顺序：框架默认 &rarr; classpath YAML general &rarr; classpath YAML method
   * &rarr; 程序化 general &rarr; 程序化 controller &rarr; 程序化 method &rarr; 运行时覆盖
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   * @return 最终生效的配置
   */
  public ApiLogConfig resolveConfig(String controllerName, String methodKey) {
    // 第1层：框架默认
    ApiLogConfig resolved = ApiLogConfig.frameDefaults();

    // 第2层：classpath YAML — Controller 级 general
    Map<String, ApiLogConfig> yamlController = yamlConfigs.get(controllerName);
    if (yamlController != null) {
      ApiLogConfig yamlGeneral = yamlController.get("__general__");
      if (yamlGeneral != null) {
        resolved = resolved.mergeFrom(yamlGeneral);
      }
      // 第3层：classpath YAML — 方法级
      ApiLogConfig yamlMethod = yamlController.get(methodKey);
      if (yamlMethod != null) {
        resolved = resolved.mergeFrom(yamlMethod);
      }
    }

    // 第4层：程序化 — 全局
    Map<String, ApiLogConfig> globalProg = programmaticConfigs.get("__global__");
    if (globalProg != null) {
      ApiLogConfig progGlobal = globalProg.get("__general__");
      if (progGlobal != null) {
        resolved = resolved.mergeFrom(progGlobal);
      }
    }

    // 第5层：程序化 — Controller 级
    Map<String, ApiLogConfig> progController = programmaticConfigs.get(controllerName);
    if (progController != null) {
      ApiLogConfig progGeneral = progController.get("__general__");
      if (progGeneral != null) {
        resolved = resolved.mergeFrom(progGeneral);
      }
      // 第6层：程序化 — 方法级
      ApiLogConfig progMethod = progController.get(methodKey);
      if (progMethod != null) {
        resolved = resolved.mergeFrom(progMethod);
      }
    }

    // 第7层：运行时动态修改
    String runtimeKey = controllerName + ":" + methodKey;
    ApiLogConfig runtimeConfig = runtimeConfigs.get(runtimeKey);
    if (runtimeConfig != null) {
      resolved = resolved.mergeFrom(runtimeConfig);
    }

    return resolved;
  }

  // ============================================================
  // 运行时动态修改 API（仅内存，重启丢失）
  // ============================================================

  /**
   * 启用指定接口方法的日志打印。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   */
  public void enable(String controllerName, String methodKey) {
    String key = controllerName + ":" + methodKey;
    ApiLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiLogConfig());
    config.setEnabled(true);
    log.info("API 日志已启用: {}.{}", controllerName, methodKey);
  }

  /**
   * 禁用指定接口方法的日志打印。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   */
  public void disable(String controllerName, String methodKey) {
    String key = controllerName + ":" + methodKey;
    ApiLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiLogConfig());
    config.setEnabled(false);
    log.info("API 日志已禁用: {}.{}", controllerName, methodKey);
  }

  /**
   * 设置指定接口方法的日志级别。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   * @param level          日志级别（TRACE/DEBUG/INFO/WARN/ERROR）
   */
  public void setLevel(String controllerName, String methodKey, String level) {
    String key = controllerName + ":" + methodKey;
    ApiLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiLogConfig());
    config.setLevel(level.toUpperCase());
    log.info("API 日志级别已更新: {}.{} → {}", controllerName, methodKey, level.toUpperCase());
  }

  /**
   * 临时开启指定接口方法的日志打印，到期自动关闭。
   * <p>
   * 对该接口的<b>所有请求</b>无条件打印日志。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   * @param duration       存活时间（到期自动关闭）
   */
  public void enableTemporarily(String controllerName, String methodKey, Duration duration) {
    String key = controllerName + ":" + methodKey;
    ApiLogConfig config = runtimeConfigs.computeIfAbsent(key, k -> new ApiLogConfig());
    config.setEnabled(true);
    config.setExpiresAt(Instant.now().plus(duration));
    log.info("API 日志临时开启: {}.{}，将在 {} 后自动关闭", controllerName, methodKey, duration);
  }

  /**
   * 更新指定接口方法的完整配置。
   *
   * @param controllerName Controller 接口名
   * @param methodKey      方法标识
   * @param config         新配置
   */
  public void updateConfig(String controllerName, String methodKey, ApiLogConfig config) {
    String key = controllerName + ":" + methodKey;
    runtimeConfigs.put(key, config);
    log.info("API 日志配置已更新: {}.{}", controllerName, methodKey);
  }

  // ============================================================
  // 条件日志管理
  // ============================================================

  /**
   * 注册一条条件触发配置。
   * <p>
   * 当 {@code config.isConditional() == true} 时，该配置会存入条件列表，
   * 拦截器将按请求参数匹配决定是否打印日志。
   * 当 {@code config.isConditional() == false} 时，该配置作为普通的运行时覆盖存入 runtimeConfigs。
   * <p>
   * 条件配置中的 {@code ttl} 为强制必填项。
   *
   * @param config 日志配置（matchType 非 null 表示条件触发）
   * @return 注册后的配置
   * @throws NullPointerException     当条件模式下 TTL 为 null
   * @throws IllegalArgumentException 当 TTL 为零或负值
   */
  public ApiLogConfig registerCondition(ApiLogConfig config) {
    if (config.isConditional()) {
      activeConditionalConfigs.add(config);
      log.info("条件日志已注册: conditionGroup={}, ttl={}",
        config.getConditionGroup(), config.getTtl());
    } else {
      log.info("非条件配置已注册（将作为运行时覆盖）: level={}", config.getLevel());
    }
    return config;
  }

  /**
   * 注销一条条件配置。
   *
   * @param config 要注销的配置
   * @return true 表示成功移除
   */
  public boolean unregisterCondition(ApiLogConfig config) {
    boolean removed = activeConditionalConfigs.remove(config);
    if (removed) {
      log.info("条件日志已注销");
    }
    return removed;
  }

  /**
   * 列出所有活跃（未过期）的条件配置。
   *
   * @return 活跃条件列表（不可变副本）
   */
  public List<ApiLogConfig> listActiveConditionalConfigs() {
    return activeConditionalConfigs.stream()
      .filter(c -> !c.isExpired())
      .collect(Collectors.toUnmodifiableList());
  }

  /**
   * 获取活跃条件列表的只读视图（用于拦截器中高效遍历匹配）。
   * <p>
   * 返回的是 {@link CopyOnWriteArrayList} 的快照引用，迭代期间不受并发写入影响。
   *
   * @return 活跃条件配置列表
   */
  public List<ApiLogConfig> getActiveConditionalConfigs() {
    return activeConditionalConfigs;
  }

  /**
   * 设置 Controller 元数据注册表（由 LabzenWebConfigurer 在初始化时调用）。
   *
   * @param registry 编译期生成的元数据注册表
   */
  public void setRegistry(LoggableControllerMetaRegistry registry) {
    this.registry = registry;
  }

  // ============================================================
  // API 端点详情查询（供业务项目 LogManagementController 使用）
  // ============================================================

  /**
   * API 端点详情记录，包含元数据信息和当前生效的日志配置。
   *
   * @param httpMethod     HTTP 方法（如 POST、GET）
   * @param urlPattern     完整 URL 模式（如 /api/user/{id}）
   * @param methodName     方法名（如 create、info）
   * @param controllerName Controller 接口简单名（如 UserController）
   * @param parameterTypes 参数类型列表
   * @param logConfig      当前生效的日志配置（三层合并后）
   */
  public record ApiEndpointDetail(
    String httpMethod,
    String urlPattern,
    String methodName,
    String controllerName,
    List<String> parameterTypes,
    ApiLogConfig logConfig
  ) {
  }

  /**
   * 获取所有已注册 API 端点的详细信息（元数据 + 当前日志配置）。
   * <p>
   * 遍历编译期生成的所有 Controller 元数据，结合 YAML 配置、程序化配置和运行时配置，
   * 返回每个端点的完整信息。业务项目可在 {@code LogManagementController} 中调用此方法，
   * 为开发者或运维人员提供 API 端点管理和日志配置查看功能。
   * <p>
   * <b>典型使用场景：</b>
   * <pre>{@code
   *   @RestController
   *   public class LogManagementController {
   *       @Autowired
   *       private ApiLogConfigManager configManager;
   *
   *       // 查看所有 API 端点及其日志配置
   *       @GetMapping("/admin/api-log/endpoints")
   *       public List<ApiEndpointDetail> listEndpoints() {
   *           return configManager.getApiEndpointsDetail();
   *       }
   *
   *       // 查看指定 Controller 的端点
   *       @GetMapping("/admin/api-log/endpoints/{controllerName}")
   *       public List<ApiEndpointDetail> listEndpoints(@PathVariable String controllerName) {
   *           return configManager.getApiEndpointsDetail(controllerName);
   *       }
   *   }
   * }</pre>
   *
   * @return 所有 API 端点详情列表（按 Controller 分组排序）
   */
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

        // 只保留方法名格式的 key（跳过 HTTP方法+URL 格式的重复条目）
        if (!methodKey.equals(methodMeta.methodName())) {
          continue;
        }

        // 解析当前生效的配置
        ApiLogConfig config = resolveConfig(controllerName, methodKey);

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

    // 按 Controller 名 + HTTP方法 + URL 排序
    result.sort(Comparator.comparing(ApiEndpointDetail::controllerName)
      .thenComparing(ApiEndpointDetail::httpMethod)
      .thenComparing(ApiEndpointDetail::urlPattern));

    return result;
  }

  /**
   * 获取指定 Controller 的所有 API 端点详细信息。
   *
   * @param controllerName Controller 接口简单名（如 "UserController"）
   * @return 该 Controller 下的所有端点详情列表
   */
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

          ApiLogConfig config = resolveConfig(controllerName, methodKey);
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

  /**
   * 获取指定端点的当前日志配置。
   * <p>
   * 等价于 {@code resolveConfig(controllerName, methodKey)} 的便捷封装。
   *
   * @param controllerName Controller 接口简单名
   * @param methodKey      方法标识（方法名 或 HTTP方法+URL）
   * @return 当前生效的日志配置
   */
  public ApiLogConfig getEndpointLogConfig(String controllerName, String methodKey) {
    return resolveConfig(controllerName, methodKey);
  }

  /**
   * 销毁管理器，关闭过期清理调度器。
   */
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
}
