package cn.labzen.web.api.log.config;

import jakarta.annotation.Nonnull;
import lombok.Data;
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
@SuppressWarnings("unused")
@Data
public class ApiLogConfig {

  /**
   * 总开关，默认关闭
   */
  private Boolean enabled;

  /**
   * 日志级别（YAML 映射字段，接收字符串如 "DEBUG"、"INFO"）。
   * <p>
   * 通过 {@link #getLevel()} 获取解析后的 {@link Level} 枚举值。
   */
  private String level;

  /**
   * level 字符串解析后的枚举缓存（懒加载）
   */
  private Level resolvedLevel;

  /**
   * 请求打印开关（独立控制）
   */
  private Boolean logRequest;

  /**
   * 响应打印开关（独立控制）
   */
  private Boolean logResponse;

  /**
   * 采样率，范围 [0.0, 1.0]，默认 1.0（全量打印）
   */
  private Double samplingRate;

  /**
   * 获取日志级别枚举值。
   * <p>
   * 将 {@code level} 字符串懒解析为 {@link Level} 枚举。解析失败时返回 {@link Level#DEBUG}。
   */
  public Level getLevel() {
    return resolvedLevel;
  }

  /**
   * 设置日志级别枚举值。
   */
  public void setLevel(Level level) {
    this.resolvedLevel = level;
    if (level != null) {
      this.level = level.name();
    }
  }

  /**
   * 设置日志级别枚举值。
   */
  public void setLevel(String level) {
    try {
      this.resolvedLevel = Level.valueOf(level);
      this.level = level;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 用 override 中的非空字段覆盖当前实例属性
   */
  public void merge(@Nonnull ApiLogConfig override) {
    this.setEnabled(value(this.getEnabled(), override.getEnabled()));
    this.setLevel(value(this.getLevel(), override.getLevel()));
    this.setLogRequest(value(this.getLogRequest(), override.getLogRequest()));
    this.setLogResponse(value(this.getLogResponse(), override.getLogResponse()));
    this.setSamplingRate(value(this.getSamplingRate(), override.getSamplingRate()));
  }

  protected <T> T value(T originalValue, T overrideValue) {
    if (overrideValue != null) {
      return overrideValue;
    }
    return originalValue;
  }
}
