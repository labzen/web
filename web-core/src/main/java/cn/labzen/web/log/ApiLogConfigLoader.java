package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import cn.labzen.web.api.log.config.ConditionGroup;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.log.bean.YamlFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static cn.labzen.web.api.definition.Constants.*;

/**
 * 只读 classpath YAML 配置加载器。
 * <p>
 * 使用 SnakeYAML 的 JavaBean 映射直接反序列化 YAML 文件到 {@link ApiLogConfig} / {@link ApiEndpointLogConfig}。
 * general → {@link ApiLogConfig}，methods → {@link ApiEndpointLogConfig}。
 * <p>
 * <b>YAML 文件格式：</b>
 * <pre>{@code
 * general:
 *   enabled: true
 *   level: DEBUG
 * methods:
 *   create:
 *     enabled: true
 *     level: INFO
 *   "a1b2c":
 *     enabled: true
 *     conditionExpression: "(username contains '张三')"
 * }</pre>
 * <p>
 * <b>设计说明：</b>{@code level} 和 {@code conditionExpression} 直接作为 YAML 映射字段，
 * 无需额外的中间 Bean。{@code ApiLogConfig.getLevel()} 和 {@code ApiEndpointLogConfig.getConditionGroup()}
 * 在运行时懒解析。conditionExpression 由 {@link ConditionExpressionParser} 解析后注入。
 *
 * @see ApiLogConfig
 * @see ApiEndpointLogConfig
 * @see ApiLogConfigManager
 */
public class ApiLogConfigLoader {

  private static final String CLASSPATH_PATTERN = "classpath*:" + API_LOG_CONFIG_DIR + "/*.yml";
  private static final Yaml YAML = new Yaml();

  private final LabzenLogger logger = Loggers.getLogger(ApiLogConfigLoader.class);
  private final LoggableControllerMetaRegistry registry;

  ApiLogConfigLoader(LoggableControllerMetaRegistry registry) {
    this.registry = registry;
  }

  // ============================================================
  // 公共入口
  // ============================================================

  /**
   * 将 conditionExpression 解析为 ConditionGroup 并注入到配置中。
   */
  private static void resolveCondition(ApiEndpointLogConfig config) {
    if (Strings.isNotBlank(config.getConditionExpression())) {
      ConditionGroup group = ConditionExpressionParser.parse(config.getConditionExpression());
      if (group != null) {
        config.setConditionGroup(group);
      }
    }
  }

  // ============================================================
  // 辅助方法
  // ============================================================

  /**
   * 加载所有 classpath 下的 API 日志 YAML 配置。
   *
   * @return Map&lt;Controller接口名, Map&lt;方法Key, ApiLogConfig&gt;&gt;
   */
  public Map<String, Map<String, ApiLogConfig>> loadAll() {
    Map<String, Map<String, ApiLogConfig>> allConfigs = new LinkedHashMap<>();

    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources;
    try {
      resources = resolver.getResources(CLASSPATH_PATTERN);
    } catch (Exception e) {
      logger.atWarn()
            .scene(LOGGER_SCENE_API_LOG_CONFIG)
            .status(Status.WRONG)
            .setCause(e)
            .log("无法扫描 API 日志配置文件目录 {}", API_LOG_CONFIG_DIR);
      return allConfigs;
    }

    for (Resource resource : resources) {
      try {
        String filename = resource.getFilename();
        if (filename == null || !Strings.endsWith(filename, false, ".yml", ".yaml")) {
          continue;
        }
        String controllerName = Strings.frontUntil(filename, ".", false);

        Optional<ControllerMeta> lookupMeta = registry.lookup(controllerName);
        if (lookupMeta.isEmpty()) {
          logger.atWarn()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.FIXME)
                .log("API 日志配置文件 [{}] 未在 Controller 元数据注册表中找到，跳过", filename);
          continue;
        }
        ControllerMeta controllerMeta = lookupMeta.get();
        String interfaceName = controllerMeta.interfaceClass().getName();

        // SnakeYAML Bean 映射：直接到 ApiLogConfig / ApiEndpointLogConfig
        YamlFile yamlFile;
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
          yamlFile = YAML.loadAs(reader, YamlFile.class);
        }

        if (yamlFile == null) {
          logger.atWarn()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.FIXME)
                .log("API 日志配置文件 [{}] 无效，跳过", filename);
          continue;
        }

        Map<String, ApiLogConfig> controllerConfigs = new LinkedHashMap<>();
        if (yamlFile.getGeneral() != null) {
          controllerConfigs.put(API_LOG_KEY_GENERAL, yamlFile.getGeneral());
        }

        // methods → ApiEndpointLogConfig
        Map<String, ApiEndpointLogConfig> methods = yamlFile.getMethods();
        if (methods != null) {
          for (Map.Entry<String, ApiEndpointLogConfig> entry : methods.entrySet()) {
            String rawKey = entry.getKey();
            ApiEndpointLogConfig methodConfig = entry.getValue();

            // 解析 conditionExpression → ConditionGroup
            resolveCondition(methodConfig);

            boolean containsKey = controllerMeta.methods().containsKey(rawKey);
            //            Optional<String> methodHash = resolveMethodNameToHash(controllerName, rawKey);
            if (containsKey) {
              String hash = controllerMeta.methods().get(rawKey).hash();
              controllerConfigs.put(hash, methodConfig);
            } else {
              logger.atWarn()
                    .scene(LOGGER_SCENE_API_LOG_CONFIG)
                    .status(Status.FIXME)
                    .log("配置 [{}] 的方法 [{}] 在 Controller 元数据中找不到，跳过", filename, rawKey);
            }
          }
        }

        allConfigs.put(interfaceName, controllerConfigs);
        if (logger.isDebugEnabled()) {
          long methodsCount = controllerConfigs.keySet().stream().filter(k -> !k.equals(API_LOG_KEY_GENERAL)).count();
          logger.atDebug()
                .scene(LOGGER_SCENE_API_LOG_CONFIG)
                .status(Status.REMIND)
                .log("已加载 API 日志配置: {} ({} 个方法级配置)", controllerName, methodsCount);
        }
      } catch (Exception e) {
        logger.atWarn()
              .scene(LOGGER_SCENE_API_LOG_CONFIG)
              .status(Status.FIXME)
              .setCause(e)
              .log("解析 API 日志配置文件 [{}] 失败", resource.getFilename());
      }
    }

    return allConfigs;
  }

  //private Optional<String> resolveMethodNameToHash(String controllerName, String key) {
  //  Optional<ControllerMeta> lookup = registry.lookup(controllerName);
  //  return lookup.map(meta -> {
  //    if (meta.methods().containsKey(key)) {
  //      return meta.methods().get(key).hash();
  //    }
  //    return null;
  //  });
  //}
  //
  //private boolean isMethodExists(String controllerName, String methodName) {
  //  return registry.lookup(controllerName).map(meta -> meta.methods().containsKey(methodName)).orElse(false);
  //}
}
