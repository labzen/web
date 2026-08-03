package cn.labzen.web.api.log.config;

import cn.labzen.tool.util.Collections;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * 端点日志配置（继承 {@link ApiLogConfig}）。
 * <p>
 * 在通用日志配置基础上，增加端点特有的属性：
 * <ul>
 *   <li>参数过滤（includeParams / excludeParams）</li>
 *   <li>响应体脱敏（responseMaskPatterns）</li>
 *   <li>条件触发（conditionExpression / conditionGroup / ttl / createdAt / expiresAt）</li>
 * </ul>
 * <p>
 * <b>设计说明：</b>{@code conditionExpression} 直接接收 YAML 中的原始条件字符串，
 * {@link #getConditionGroup()} 懒解析为 {@link ConditionGroup} 树。这避免了为 SnakeYAML 映射创建冗余 Bean。
 * <p>
 * 条件触发模式（{@code conditionExpression != null}）：仅对请求参数满足条件的请求打印日志。
 * 无条件模式（{@code conditionExpression == null}）：{@code enabled=true} 时对所有请求打印。
 *
 * @see ApiLogConfig
 * @see ConditionGroup
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ApiEndpointLogConfig extends ApiLogConfig {

  // ============================================================
  // 端点特有字段
  // ============================================================

  /**
   * 仅打印指定参数（白名单），与 excludeParams 互斥，includeParams 非空时优先
   */
  private Set<String> includeParams = Sets.newHashSet();

  /**
   * 排除指定参数（黑名单）
   */
  private Set<String> excludeParams = Sets.newHashSet();

  /**
   * 响应体脱敏规则：JSON Path → 脱敏规则名称（如 "phone-mask"、"idcard-mask"）
   */
  private Map<String, String> responseMaskPatterns = Maps.newHashMap();

  // ============================================================
  // 条件触发字段
  // ============================================================

  /**
   * 条件表达式原始字符串（YAML 映射字段，如 {@code "username contains '张三' AND status = active"}）。
   * <p>
   * 通过 {@link #getConditionGroup()} 获取解析后的条件树。
   */
  private String conditionExpression;

  /**
   * conditionExpression 解析后的条件树缓存（懒加载）。
   * <p>
   * 在 web-core 模块中通过 {@code ConditionExpressionParser.parse()} 解析，
   * 此处仅作为缓存字段，不直接暴露给 YAML 映射。
   */
  private ConditionGroup resolvedConditionGroup;

  /**
   * 条件存活时间，条件模式时强制必填
   */
  private Duration ttl;

  /**
   * 条件创建时间
   */
  private Instant createdAt;

  /**
   * 过期时间
   */
  private Instant expiresAt;

  // ============================================================
  // 条件组获取
  // ============================================================

  /**
   * 获取条件规则树。
   * <p>
   * 当 {@code conditionExpression} 不为空且 {@code resolvedConditionGroup} 尚未解析时，
   * 调用方（{@code ApiLogConfigLoader}）应通过 {@link #setConditionGroup(ConditionGroup)} 注入解析结果。
   * 此方法仅返回缓存值。
   *
   * @return 解析后的条件树，未解析时为 null
   */
  public ConditionGroup getConditionGroup() {
    return resolvedConditionGroup;
  }

  /**
   * 注入已解析的条件树（由 {@code ApiLogConfigLoader} 在加载时调用）。
   */
  public void setConditionGroup(ConditionGroup group) {
    this.resolvedConditionGroup = group;
  }

  // ============================================================
  // 配置合并方法
  // ============================================================

  /**
   * 用 {@code override} 中的非默认值字段覆盖当前实例，返回新实例。
   * <p>
   * 用于三层配置合并（框架默认 → YAML → 程序化 → 运行时）。
   * 支持 {@link ApiLogConfig} 和 {@link ApiEndpointLogConfig} 两种覆盖源。
   */
  public ApiEndpointLogConfig mergeFrom(ApiLogConfig override) {
    if (override == null) return this;

    ApiEndpointLogConfig merged = new ApiEndpointLogConfig();
    // 通用字段
    merged.setEnabled(override.isEnabled());
    merged.setLevel(override.getLevel());
    merged.setLogRequestParams(override.isLogRequestParams());
    merged.setLogResponseBody(override.isLogResponseBody());
    merged.setLogException(override.isLogException());
    merged.setSamplingRate(override.getSamplingRate());

    if (override instanceof ApiEndpointLogConfig ep) {
      // 端点特有字段：非空时采用
      merged.includeParams = Collections.isNullOrEmpty(ep.includeParams) ? this.includeParams : ep.includeParams;
      merged.excludeParams = Collections.isNullOrEmpty(ep.excludeParams) ? this.excludeParams : ep.excludeParams;
      merged.responseMaskPatterns = ep.responseMaskPatterns != null && !ep.responseMaskPatterns.isEmpty()
        ? ep.responseMaskPatterns : this.responseMaskPatterns;
      merged.expiresAt = ep.expiresAt != null ? ep.expiresAt : this.expiresAt;
      merged.ttl = ep.ttl != null ? ep.ttl : this.ttl;
      merged.createdAt = ep.createdAt != null ? ep.createdAt : this.createdAt;

      // 条件表达式：非空时采用
      if (ep.conditionExpression != null && !ep.conditionExpression.isEmpty()) {
        merged.conditionExpression = ep.conditionExpression;
        merged.resolvedConditionGroup = ep.resolvedConditionGroup;
      } else {
        merged.conditionExpression = this.conditionExpression;
        merged.resolvedConditionGroup = this.resolvedConditionGroup;
      }
    }
    return merged;
  }

  // ============================================================
  // 便捷方法
  // ============================================================

  public boolean isConditional() {
    return resolvedConditionGroup != null || (conditionExpression != null && !conditionExpression.isEmpty());
  }

  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }
}
