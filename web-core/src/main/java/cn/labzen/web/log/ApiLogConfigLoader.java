package cn.labzen.web.log;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import cn.labzen.web.api.log.config.ConditionGroup;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.log.bean.YamlFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static cn.labzen.web.api.definition.Constants.API_LOG_CONFIG_DIR;
import static cn.labzen.web.api.definition.Constants.API_LOG_KEY_GENERAL;

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
@Slf4j
public class ApiLogConfigLoader {

  private static final String CLASSPATH_PATTERN = "classpath*:" + API_LOG_CONFIG_DIR + "/*.yml";
  private static final Pattern HASH_KEY_PATTERN = Pattern.compile("^[0-9a-fA-F]{5}$");
  private static final Yaml YAML = new Yaml();

  private final LoggableControllerMetaRegistry registry;

  ApiLogConfigLoader(LoggableControllerMetaRegistry registry) {
    this.registry = registry;
  }

  // ============================================================
  // 公共入口
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
      logger.warn("无法扫描 API 日志配置文件目录 [{}]: {}", API_LOG_CONFIG_DIR, e.getMessage());
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
          logger.warn("API 日志配置文件 [{}] 对应的 Controller 未在元数据注册表中找到，跳过", filename);
          continue;
        }
        ControllerMeta controllerMeta = lookupMeta.get();
        String interfaceName = controllerMeta.interfaceClass().getName();

        // SnakeYAML Bean 映射：直接到 ApiLogConfig / ApiEndpointLogConfig
        YamlFile yamlFile;
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
          yamlFile = YAML.loadAs(reader, YamlFile.class);
        }

        if (yamlFile == null || yamlFile.general == null) {
          logger.warn("API 日志配置文件 [{}] 的 general 配置无效，跳过", filename);
          continue;
        }

        Map<String, ApiLogConfig> controllerConfigs = new LinkedHashMap<>();
        controllerConfigs.put(API_LOG_KEY_GENERAL, yamlFile.general);

        // methods → ApiEndpointLogConfig
        Map<String, ApiEndpointLogConfig> methods = yamlFile.methods;
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
              logger.warn("配置文件 [{}] 的方法 [{}] 未能在 Controller 的元数据中未找到对应方法，跳过",
                  filename,
                  rawKey);
            }

            //            if (HASH_KEY_PATTERN.matcher(rawKey).matches()) {
            //              Optional<String> methodNameOpt = resolveMethodNameToHash(registry, controllerName, rawKey);
            //              if (methodNameOpt.isEmpty()) {
            //                logger.warn("配置的哈希 key [{}] 在 Controller [{}] 的元数据中未找到对应方法，跳过", rawKey, controllerName);
            //                continue;
            //              }
            //              controllerConfigs.put(rawKey, methodConfig);
            //              controllerConfigs.putIfAbsent(methodNameOpt.get(), methodConfig);
            //              logger.debug("哈希 key [{}] 已解析为方法名 [{}]，同时注册两个映射", rawKey, methodNameOpt.get());
            //            } else {
            //              if (!isMethodExists(registry, controllerName, rawKey)) {
            //                logger.warn("配置的方法名 [{}] 在 Controller [{}] 的元数据中未找到，跳过", rawKey, controllerName);
            //                continue;
            //              }
            //              controllerConfigs.put(rawKey, methodConfig);
            //            }
          }
        }

        allConfigs.put(interfaceName, controllerConfigs);
        logger.debug("已加载 API 日志配置: {} ({} 个方法级配置)", controllerName, controllerConfigs.size() - 1);
      } catch (Exception e) {
        logger.warn("解析 API 日志配置文件 [{}] 失败: {}", resource.getFilename(), e.getMessage());
      }
    }

    return allConfigs;
  }

  // ============================================================
  // 辅助方法
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

  private Optional<String> resolveMethodNameToHash(String controllerName, String key) {
    Optional<ControllerMeta> lookup = registry.lookup(controllerName);
    return lookup.map(meta -> {
      if (meta.methods().containsKey(key)) {
        return meta.methods().get(key).hash();
      }
      return null;
    });
    //
    //    return registry.lookup(controllerName)
    //      .flatMap(meta -> Optional.ofNullable(meta.methods().get(key)))
    //      .map(ControllerMethodMeta::methodName);
  }

  private boolean isMethodExists(String controllerName, String methodName) {
    return registry.lookup(controllerName).map(meta -> meta.methods().containsKey(methodName)).orElse(false);
  }
}
