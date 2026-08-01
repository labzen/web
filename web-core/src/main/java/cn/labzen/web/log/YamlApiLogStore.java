package cn.labzen.web.log;

import cn.labzen.web.api.log.ApiLogConfig;
import cn.labzen.web.api.log.ConditionGroup;
import cn.labzen.web.api.log.ConditionRule;
import cn.labzen.web.api.log.LogicOperator;
import cn.labzen.web.api.log.MatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static cn.labzen.web.api.definition.Constants.API_LOG_CONFIG_DIR;

/**
 * 只读 classpath YAML 配置加载器。
 * <p>
 * 在应用启动时，扫描 {@code classpath:labzen-web/*.yml} 目录下的所有 YAML 配置文件，
 * 将其解析为 {@link ApiLogConfig} 结构并按 Controller 接口名组织。
 * <p>
 * <b>YAML 文件格式：</b>
 * <pre>{@code
 * general:
 *   enabled: true
 *   level: DEBUG
 *   samplingRate: 0.5
 *   logRequestParams: true
 *   logResponseBody: false
 *   excludeParams:
 *     - password
 *     - token
 * methods:
 *   create:
 *     enabled: true
 *     level: INFO
 *     includeParams:
 *       - name
 *       - email
 *   find:
 *     enabled: true
 *     level: DEBUG
 *     ttl: PT5M              # ISO-8601 Duration 格式，5分钟
 *     condition: "(username contains '张三' AND status = active) OR amount > 1000"
 * }</pre>
 * <p>
 * <b>设计约束：</b>仅负责从 classpath 读取配置，不提供写入能力。
 * Spring Boot JAR 部署后 classpath 只读，运行时动态修改仅存内存。
 *
 * @see ApiLogConfig
 * @see ApiLogConfigManager
 */
public class YamlApiLogStore {

  private static final Logger log = LoggerFactory.getLogger(YamlApiLogStore.class);

  /** YAML 配置文件搜索路径 */
  private static final String CLASSPATH_PATTERN = "classpath*:" + API_LOG_CONFIG_DIR + "/*.yml";

  /** YAML 解析器（SnakeYAML，Spring Boot 内置，线程安全） */
  private static final Yaml YAML = new Yaml();

  /**
   * 加载所有 classpath 下的 API 日志 YAML 配置。
   * <p>
   * 扫描 {@code classpath:labzen-web/*.yml}，将文件名（去掉 .yml 后缀）作为 Controller 接口名，
   * 解析 YAML 内容为 general 配置和 methods Map。
   *
   * @return Map&lt;Controller接口名, Map&lt;方法Key, ApiLogConfig&gt;&gt;，
   *         每个 Controller 的 general 配置存储在 key 为 "__general__" 的条目中
   */
  @SuppressWarnings("unchecked")
  public Map<String, Map<String, ApiLogConfig>> loadAll() {
    Map<String, Map<String, ApiLogConfig>> allConfigs = new LinkedHashMap<>();

    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources;
    try {
      resources = resolver.getResources(CLASSPATH_PATTERN);
    } catch (Exception e) {
      log.warn("无法扫描 API 日志配置文件目录 [{}]: {}", API_LOG_CONFIG_DIR, e.getMessage());
      return allConfigs;
    }

    for (Resource resource : resources) {
      try {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".yml")) {
          continue;
        }

        // 文件名（去掉 .yml 后缀）即 Controller 接口名
        String controllerName = filename.substring(0, filename.length() - 4);

        // 解析 YAML 为 Map 结构
        Map<String, Object> yamlData;
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
          yamlData = YAML.loadAs(reader, Map.class);
        }

        if (yamlData == null) {
          log.warn("API 日志配置文件 [{}] 内容为空，跳过", filename);
          continue;
        }

        // 解析 general 通用配置
        Map<String, ApiLogConfig> controllerConfigs = new LinkedHashMap<>();
        Object generalObj = yamlData.get("general");
        if (generalObj instanceof Map<?, ?> generalMap) {
          ApiLogConfig generalConfig = parseApiLogConfig((Map<String, Object>) generalMap);
          controllerConfigs.put("__general__", generalConfig);
        } else {
          // 无 general 配置时使用默认值
          controllerConfigs.put("__general__", ApiLogConfig.frameDefaults());
        }

        // 解析 methods 配置
        Object methodsObj = yamlData.get("methods");
        if (methodsObj instanceof Map<?, ?> methodsMap) {
          for (Map.Entry<?, ?> entry : methodsMap.entrySet()) {
            String methodKey = String.valueOf(entry.getKey());
            if (entry.getValue() instanceof Map<?, ?> methodConfigMap) {
              ApiLogConfig methodConfig = parseApiLogConfig((Map<String, Object>) methodConfigMap);
              controllerConfigs.put(methodKey, methodConfig);
            }
          }
        }

        allConfigs.put(controllerName, controllerConfigs);
        log.info("已加载 API 日志配置: {} ({} 个方法级配置)", controllerName, controllerConfigs.size() - 1);
      } catch (Exception e) {
        log.warn("解析 API 日志配置文件 [{}] 失败: {}", resource.getFilename(), e.getMessage());
      }
    }

    return allConfigs;
  }

  /**
   * 将 YAML Map 结构解析为 {@link ApiLogConfig} 实例。
   * <p>
   * 支持的 YAML 字段映射：
   * <ul>
   *   <li>{@code enabled} → {@link ApiLogConfig#setEnabled(boolean)}</li>
   *   <li>{@code level} → {@link ApiLogConfig#setLevel(String)}</li>
   *   <li>{@code logRequestParams} → {@link ApiLogConfig#setLogRequestParams(boolean)}</li>
   *   <li>{@code logResponseBody} → {@link ApiLogConfig#setLogResponseBody(boolean)}</li>
   *   <li>{@code logException} → {@link ApiLogConfig#setLogException(boolean)}</li>
   *   <li>{@code samplingRate} → {@link ApiLogConfig#setSamplingRate(double)}</li>
   *   <li>{@code includeParams} → {@link ApiLogConfig#setIncludeParams(Set)}</li>
   *   <li>{@code excludeParams} → {@link ApiLogConfig#setExcludeParams(Set)}</li>
 *   <li>{@code responseMaskPatterns} → {@link ApiLogConfig#setResponseMaskPatterns(Map)}</li>
 *   <li>{@code condition} → 语义化条件表达式，解析为 {@link ConditionGroup} 树</li>
 *   <li>{@code ttl} → {@link ApiLogConfig#setTtl(Duration)}（ISO-8601 Duration 格式，如 PT5M）</li>
 * </ul>
   *
   * @param raw YAML 解析后的 Map
   * @return 配置实例（未设置的字段保持框架默认值）
   */
  @SuppressWarnings("unchecked")
  private ApiLogConfig parseApiLogConfig(Map<String, Object> raw) {
    ApiLogConfig config = new ApiLogConfig();

    if (raw.containsKey("enabled")) {
      config.setEnabled(Boolean.TRUE.equals(raw.get("enabled")));
    }
    if (raw.containsKey("level")) {
      config.setLevel(String.valueOf(raw.get("level")));
    }
    if (raw.containsKey("logRequestParams")) {
      config.setLogRequestParams(Boolean.TRUE.equals(raw.get("logRequestParams")));
    }
    if (raw.containsKey("logResponseBody")) {
      config.setLogResponseBody(Boolean.TRUE.equals(raw.get("logResponseBody")));
    }
    if (raw.containsKey("logException")) {
      config.setLogException(Boolean.TRUE.equals(raw.get("logException")));
    }
    if (raw.containsKey("samplingRate")) {
      Object rate = raw.get("samplingRate");
      if (rate instanceof Number num) {
        config.setSamplingRate(num.doubleValue());
      }
    }
    if (raw.containsKey("includeParams")) {
      Object includeObj = raw.get("includeParams");
      if (includeObj instanceof List<?> list) {
        Set<String> includeParams = new HashSet<>();
        for (Object item : list) {
          includeParams.add(String.valueOf(item));
        }
        config.setIncludeParams(includeParams);
      }
    }
    if (raw.containsKey("excludeParams")) {
      Object excludeObj = raw.get("excludeParams");
      if (excludeObj instanceof List<?> list) {
        Set<String> excludeParams = new HashSet<>();
        for (Object item : list) {
          excludeParams.add(String.valueOf(item));
        }
        config.setExcludeParams(excludeParams);
      }
    }
    if (raw.containsKey("responseMaskPatterns")) {
      Object maskObj = raw.get("responseMaskPatterns");
      if (maskObj instanceof Map<?, ?> maskMap) {
        Map<String, String> patterns = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : maskMap.entrySet()) {
          patterns.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        config.setResponseMaskPatterns(patterns);
      }
    }

    // === 条件触发字段解析 ===
    if (raw.containsKey("condition")) {
      String expression = String.valueOf(raw.get("condition"));
      ConditionGroup group = parseConditionExpression(expression);
      if (group != null) {
        config.setConditionGroup(group);
      }
    }

    // TTL
    if (raw.containsKey("ttl")) {
      String ttlStr = String.valueOf(raw.get("ttl"));
      try {
        config.setTtl(Duration.parse(ttlStr));
        config.setCreatedAt(java.time.Instant.now());
        config.setExpiresAt(java.time.Instant.now().plus(config.getTtl()));
      } catch (Exception e) {
        log.warn("无效的 ttl 值 [{}]，已忽略", ttlStr);
      }
    }

    return config;
  }

  // ============================================================
  // 语义化条件表达式解析器
  // ============================================================

  /**
   * 解析语义化条件表达式为 {@link ConditionGroup} 树。
   * <p>
   * 将 {@code "(username contains '张三' AND status = active) OR amount > 1000"}
   * 解析为：
   * <pre>{@code
   *   ConditionGroup(OR, [
   *     ConditionGroup(AND, [
   *       leaf(username,contains,张三),
   *       leaf(status,=,active)
   *     ]),
   *     leaf(amount,>,1000)
   *   ])
   * }</pre>
   */
  private ConditionGroup parseConditionExpression(String expression) {
    if (expression == null || expression.isBlank()) {
      return null;
    }

    List<String> tokens = tokenize(expression.trim());
    if (tokens.isEmpty()) {
      return null;
    }

    int[] pos = {0};
    ConditionGroup result = parseOrExpr(tokens, pos);

    if (result == null) {
      log.warn("条件表达式解析失败 [{}]", expression);
    }
    return result;
  }

  /**
   * 解析 OR 表达式 → ConditionGroup(OR, [...])。
   */
  private ConditionGroup parseOrExpr(List<String> tokens, int[] pos) {
    List<ConditionGroup> children = new ArrayList<>();
    children.add(parseAndExpr(tokens, pos));

    while (pos[0] < tokens.size() && "OR".equalsIgnoreCase(tokens.get(pos[0]))) {
      pos[0]++; // skip OR
      children.add(parseAndExpr(tokens, pos));
    }

    return children.size() == 1 ? children.get(0) : ConditionGroup.or(children);
  }

  /**
   * 解析 AND 表达式 → ConditionGroup(AND, [...])。
   */
  private ConditionGroup parseAndExpr(List<String> tokens, int[] pos) {
    List<ConditionGroup> children = new ArrayList<>();
    children.add(parsePrimary(tokens, pos));

    while (pos[0] < tokens.size() && "AND".equalsIgnoreCase(tokens.get(pos[0]))) {
      pos[0]++; // skip AND
      children.add(parsePrimary(tokens, pos));
    }

    return children.size() == 1 ? children.get(0) : ConditionGroup.and(children);
  }

  /**
   * 解析基本单元：括号子表达式 或 叶子规则。
   */
  private ConditionGroup parsePrimary(List<String> tokens, int[] pos) {
    if (pos[0] >= tokens.size()) {
      return null;
    }

    String token = tokens.get(pos[0]);
    if (")".equals(token) || "OR".equalsIgnoreCase(token) || "AND".equalsIgnoreCase(token)) {
      return null;
    }

    // 括号子表达式
    if ("(".equals(token)) {
      pos[0]++; // skip (
      ConditionGroup inner = parseOrExpr(tokens, pos);
      if (pos[0] < tokens.size() && ")".equals(tokens.get(pos[0]))) {
        pos[0]++; // skip )
      }
      return inner;
    }

    // 叶子规则
    ConditionRule rule = parseSingleConditionFromTokens(tokens, pos);
    return rule != null ? ConditionGroup.leaf(rule) : null;
  }

  /**
   * 从 token 流中解析单条条件规则。
   */
  private ConditionRule parseSingleConditionFromTokens(List<String> tokens, int[] pos) {
    if (pos[0] >= tokens.size()) return null;

    String paramName = tokens.get(pos[0]++);
    if (pos[0] >= tokens.size()) return null;

    String maybeOp = tokens.get(pos[0]);
    if ("AND".equalsIgnoreCase(maybeOp) || "OR".equalsIgnoreCase(maybeOp)
        || "(".equals(maybeOp) || ")".equals(maybeOp)) {
      pos[0]--;
      return null;
    }

    String operator = tokens.get(pos[0]++);
    String matchValue = null;
    if (pos[0] < tokens.size()) {
      String next = tokens.get(pos[0]);
      if (!"AND".equalsIgnoreCase(next) && !"OR".equalsIgnoreCase(next)
          && !"(".equals(next) && !")".equals(next)) {
        matchValue = stripQuotes(tokens.get(pos[0]++));
      }
    }

    MatchType matchType = MatchType.fromOperator(operator);
    if (matchType == null) {
      log.warn("无法识别的条件运算符 [{}]，已跳过", operator);
      return null;
    }

    if (matchType == MatchType.EXISTS) {
      return new ConditionRule(matchType, paramName, null);
    }
    return new ConditionRule(matchType, paramName, matchValue);
  }

  // ============================================================
  // Token 工具方法
  // ============================================================

  /**
   * 将表达式字符串拆分为 token 列表。
   * <p>
   * 支持：
   * <ul>
   *   <li>单引号包裹的值视为一个 token（如 {@code '张三 李四'}）</li>
   *   <li>括号视为独立 token</li>
   *   <li>空格分隔其他 token</li>
   * </ul>
   */
  private static List<String> tokenize(String expression) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuote = false;

    for (int i = 0; i < expression.length(); i++) {
      char c = expression.charAt(i);

      if (c == '\'') {
        inQuote = !inQuote;
        current.append(c);
      } else if (c == '(' || c == ')') {
        // 括号作为独立 token
        if (current.length() > 0) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        tokens.add(String.valueOf(c));
      } else if (c == ' ' && !inQuote) {
        if (current.length() > 0) {
          tokens.add(current.toString());
          current.setLength(0);
        }
      } else {
        current.append(c);
      }
    }

    if (current.length() > 0) {
      tokens.add(current.toString());
    }

    return tokens;
  }

  /**
   * 去除字符串两端的单引号或双引号。
   */
  private static String stripQuotes(String value) {
    if (value == null || value.length() < 2) {
      return value;
    }
    char first = value.charAt(0);
    char last = value.charAt(value.length() - 1);
    if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }
}
