package cn.labzen.web.api.log.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

/**
 * 通用日志配置（适用于 Controller 级别和端点级别）。
 * <p>
 * 包含日志打印的核心开关参数，由 {@link ApiEndpointLogConfig} 继承。
 * general 配置中仅包含这些字段，methods 下的端点配置在此基础上扩展。
 * <p>
 * <b>设计说明：</b>{@code level} 字段为 String 类型，直接接收 YAML 中的字符串值（如 "DEBUG"），
 * {@link #getLevel()} 将其懒转换为 {@link Level} 枚举。这避免了为 SnakeYAML 映射创建冗余 Bean。
 */
@Getter
@Setter
public class ApiLogConfig {

  /**
   * 总开关，默认关闭
   */
  private boolean enabled = false;

  /**
   * 日志级别（YAML 映射字段，接收字符串如 "DEBUG"、"INFO"）。
   * <p>
   * 通过 {@link #getLevel()} 获取解析后的 {@link Level} 枚举值。
   */
  private String level = "DEBUG";

  /**
   * level 字符串解析后的枚举缓存（懒加载）
   */
  private Level resolvedLevel;

  /**
   * 请求体打印开关（独立控制）
   */
  private boolean logRequestParams = true;

  /**
   * 响应体打印开关（独立控制）
   */
  private boolean logResponseBody = true;

  /**
   * 异常日志开关
   */
  private boolean logException = true;

  /**
   * 采样率，范围 [0.0, 1.0]，默认 1.0（全量打印）
   */
  private double samplingRate = 1.0;

  /**
   * 获取日志级别枚举值。
   * <p>
   * 将 {@code level} 字符串懒解析为 {@link Level} 枚举。解析失败时返回 {@link Level#DEBUG}。
   */
  public Level getLevel() {
    if (resolvedLevel != null) {
      return resolvedLevel;
    }
    if (level == null || level.isEmpty()) {
      resolvedLevel = Level.DEBUG;
      return resolvedLevel;
    }
    try {
      resolvedLevel = Level.valueOf(level.toUpperCase());
    } catch (IllegalArgumentException e) {
      resolvedLevel = Level.DEBUG;
    }
    return resolvedLevel;
  }

  /**
   * 设置日志级别枚举值（同时更新 level 字符串以保持一致性）。
   */
  public void setLevel(Level level) {
    this.resolvedLevel = level;
    this.level = level != null ? level.name() : "DEBUG";
  }
}
