package cn.labzen.web.api.log.config;

import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiEndpointLogConfig extends ApiLogConfig {

  // ============================================================
  // 端点特有字段
  // ============================================================

  /**
   * 仅打印指定参数（白名单），与 excludeParams 互斥，includeParams 非空时优先
   */
  private Set<String> includeParams;

  /**
   * 排除指定参数（黑名单）
   */
  private Set<String> excludeParams;

  /**
   * 响应体脱敏规则：JSON Path → 脱敏规则名称（如 "phone-mask"、"idcard-mask"）
   */
  private Map<String, String> responseMaskPatterns;

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
   * 用 override 中的非空字段覆盖当前实例属性
   */
  public void merge(@Nonnull ApiEndpointLogConfig override) {
    super.merge(override);

    this.setIncludeParams(value(this.getIncludeParams(), override.getIncludeParams()));
    this.setExcludeParams(value(this.getExcludeParams(), override.getExcludeParams()));
    this.setResponseMaskPatterns(value(this.getResponseMaskPatterns(), override.getResponseMaskPatterns()));
    this.setConditionExpression(value(this.getConditionExpression(), override.getConditionExpression()));
    this.setConditionGroup(value(this.getConditionGroup(), override.getConditionGroup()));
    this.setTtl(value(this.getTtl(), override.getTtl()));
    this.setCreatedAt(value(this.getCreatedAt(), override.getCreatedAt()));
    this.setExpiresAt(value(this.getExpiresAt(), override.getExpiresAt()));
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
