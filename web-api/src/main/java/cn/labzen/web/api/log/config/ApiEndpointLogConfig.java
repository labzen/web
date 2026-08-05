package cn.labzen.web.api.log.config;

import cn.labzen.tool.util.Collections;
import jakarta.annotation.Nonnull;
import lombok.Data;
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
   * 用 {@code override} 中的非默认值字段覆盖当前实例，返回新实例。
   * <p>
   * 用于三层配置合并（框架默认 → YAML → 程序化 → 运行时）。
   * 基于 {@code this} 复制所有字段，仅用 {@code override} 中显式设置的值覆盖。
   * boolean 类型以 {@code true} 为"显式设置"，{@code false} 保留原值；
   * {@code samplingRate} 不等于 0.0 时为"显式设置"；
   * 端点特有字段以非空为"显式设置"。
   */
//  public ApiEndpointLogConfig mergeFrom(ApiLogConfig override) {
//    if (override == null) return this;
//
//    ApiEndpointLogConfig merged = new ApiEndpointLogConfig();
//    // 先复制 this 的全部字段
//    merged.setEnabled(this.isEnabled());
//    merged.setLevel(this.getLevel());
//    merged.setLogRequest(this.isLogRequestParams());
//    merged.setLogResponse(this.isLogResponseBody());
//    merged.setLogException(this.isLogException());
//    merged.setSamplingRate(this.getSamplingRate());
//    merged.includeParams = new java.util.HashSet<>(this.includeParams);
//    merged.excludeParams = new java.util.HashSet<>(this.excludeParams);
//    merged.responseMaskPatterns = new java.util.LinkedHashMap<>(this.responseMaskPatterns);
//    merged.expiresAt = this.expiresAt;
//    merged.ttl = this.ttl;
//    merged.createdAt = this.createdAt;
//    merged.conditionExpression = this.conditionExpression;
//    merged.resolvedConditionGroup = this.resolvedConditionGroup;
//
//    // 然后用 override 中显式设置的值覆盖
//    if (override.isEnabled()) merged.setEnabled(true);
//    if (!"DEBUG".equals(override.getLevel().name()) || override instanceof ApiEndpointLogConfig) {
//      merged.setLevel(override.getLevel());
//    }
//    if (!override.isLogRequestParams()) merged.setLogRequest(false);
//    if (!override.isLogResponseBody()) merged.setLogResponse(false);
//    if (!override.isLogException()) merged.setLogException(false);
//    if (override.getSamplingRate() != 0.0) merged.setSamplingRate(override.getSamplingRate());
//
//    if (override instanceof ApiEndpointLogConfig ep) {
//      if (!Collections.isNullOrEmpty(ep.includeParams)) merged.includeParams = ep.includeParams;
//      if (!Collections.isNullOrEmpty(ep.excludeParams)) merged.excludeParams = ep.excludeParams;
//      if (ep.responseMaskPatterns != null && !ep.responseMaskPatterns.isEmpty())
//        merged.responseMaskPatterns = ep.responseMaskPatterns;
//      if (ep.expiresAt != null) merged.expiresAt = ep.expiresAt;
//      if (ep.ttl != null) merged.ttl = ep.ttl;
//      if (ep.createdAt != null) merged.createdAt = ep.createdAt;
//      if (ep.conditionExpression != null && !ep.conditionExpression.isEmpty()) {
//        merged.conditionExpression = ep.conditionExpression;
//        merged.resolvedConditionGroup = ep.resolvedConditionGroup;
//      }
//    }
//    return merged;
//  }

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
