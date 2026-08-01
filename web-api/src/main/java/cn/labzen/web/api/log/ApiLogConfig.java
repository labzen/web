package cn.labzen.web.api.log;

import cn.labzen.tool.util.Collections;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import org.slf4j.event.Level;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * API 日志配置类（普通可变 POJO）。
 * <p>
 * 统一描述一个 API 方法或 Controller 级别的日志打印配置。支持两种触发模式：
 * <ul>
 *   <li><b>无条件模式</b>（{@code conditionGroup == null}）：{@code enabled=true} 时对所有请求打印日志</li>
 *   <li><b>条件触发模式</b>（{@code conditionGroup != null}）：{@code enabled=true} 时仅对满足条件的请求打印日志，TTL 为强制必填</li>
 * </ul>
 * <p>
 * <b>条件规则树（{@code conditionGroup}）：</b>自描述 AND/OR 嵌套关系。
 * <pre>{@code
 *   // (A AND B) OR C 的树形结构：
 *   ConditionGroup(OR, [
 *     ConditionGroup(AND, [leaf(A), leaf(B)]),
 *     leaf(C)
 *   ])
 * }</pre>
 *
 * @see MatchType
 * @see ConditionRule
 * @see ConditionGroup
 */
@Data
public class ApiLogConfig {

  // ============================================================
  // 字段定义
  // ============================================================

  private boolean enabled = false;
  private Level level = Level.DEBUG;
  private boolean logRequestParams = true;
  private boolean logResponseBody = true;
  private boolean logException = true;
  private double samplingRate = 1.0;
  private Set<String> includeParams = Sets.newHashSet();
  private Set<String> excludeParams = Sets.newHashSet();
  private Map<String, String> responseMaskPatterns = Maps.newHashMap();
  private Instant expiresAt;

  // ============================================================
  // 条件触发字段
  // ============================================================

  /**
   * 条件规则树（自描述 AND/OR 嵌套关系）。
   * <p>
   * null 表示无条件模式。非 null 表示条件触发模式。
   */
  private ConditionGroup conditionGroup;

  /**
   * 条件存活时间，条件模式时强制必填
   */
  private Duration ttl;

  /**
   * 条件创建时间
   */
  private Instant createdAt;

  // ============================================================
  // 静态工厂方法
  // ============================================================

  public static ApiLogConfig frameDefaults() {
    return new ApiLogConfig();
  }

  /**
   * 创建条件配置。
   */
//  public static ApiLogConfig conditional(
//    ConditionGroup conditionGroup,
//    Duration ttl, String level, boolean logRequestParams, boolean logResponseBody
//  ) {
//    Objects.requireNonNull(ttl, "条件日志的 TTL 为必填项，不接受 null");
//    if (ttl.isNegative() || ttl.isZero()) {
//      throw new IllegalArgumentException("TTL 必须为正值，当前值: " + ttl);
//    }
//    Instant now = Instant.now();
//    ApiLogConfig config = new ApiLogConfig();
//    config.enabled = true;
//    config.level = level;
//    config.logRequestParams = logRequestParams;
//    config.logResponseBody = logResponseBody;
//    config.samplingRate = 1.0;
//    config.conditionGroup = conditionGroup;
//    config.ttl = ttl;
//    config.createdAt = now;
//    config.expiresAt = now.plus(ttl);
//    return config;
//  }

  // ============================================================
  // 配置合并方法
  // ============================================================
  public ApiLogConfig mergeFrom(ApiLogConfig override) {
    if (override == null) return this;

    ApiLogConfig merged = new ApiLogConfig();
    merged.enabled = override.enabled;
    merged.logRequestParams = override.logRequestParams;
    merged.logResponseBody = override.logResponseBody;
    merged.logException = override.logException;
    merged.level = override.level;
    merged.samplingRate = override.samplingRate;
    merged.includeParams = Collections.isNullOrEmpty(override.includeParams) ? this.includeParams : override.includeParams;
    merged.excludeParams = Collections.isNullOrEmpty(override.excludeParams) ? this.excludeParams : override.excludeParams;
    merged.responseMaskPatterns = override.responseMaskPatterns != null && !override.responseMaskPatterns.isEmpty()
      ? override.responseMaskPatterns : this.responseMaskPatterns;
    merged.expiresAt = override.expiresAt != null ? override.expiresAt : this.expiresAt;
    merged.conditionGroup = override.conditionGroup != null ? override.conditionGroup : this.conditionGroup;
    merged.ttl = override.ttl != null ? override.ttl : this.ttl;
    merged.createdAt = override.createdAt != null ? override.createdAt : this.createdAt;
    return merged;
  }

  // ============================================================
  // 便捷方法
  // ============================================================

  public boolean isConditional() {
    return conditionGroup != null;
  }

  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }
}
