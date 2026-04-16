package cn.labzen.web.exception;

import cn.labzen.meta.exception.LabzenRuntimeException;
import lombok.Getter;

/**
 * 请求异常基类
 * <p>
 * 用于表示业务层面的请求错误，支持自定义错误码和日志控制。
 * <p>
 * 特性：
 * <ul>
 *   <li>支持自定义 HTTP 状态码</li>
 *   <li>支持消息模板和参数格式化</li>
 *   <li>支持控制是否记录日志（避免重复打印）</li>
 * </ul>
 *
 * @see LabzenRuntimeException
 */
@Getter
public class RequestException extends LabzenRuntimeException {

  /** 业务错误码 */
  private final int code;
  /** 是否记录日志 */
  private final boolean logging;

  // ========== 基础构造方法 ==========

  /**
   * 创建请求异常
   *
   * @param code    错误码
   * @param message 错误消息
   */
  public RequestException(int code, String message) {
    super(message);
    this.code = code;
    this.logging = true;
  }

  // ========== 带日志控制的构造方法 ==========

  /**
   * 创建请求异常（带日志控制）
   *
   * @param code    错误码
   * @param logging 是否记录日志
   * @param message 错误消息
   */
  public RequestException(int code, boolean logging, String message) {
    super(message);
    this.code = code;
    this.logging = logging;
  }

  // ========== 格式化消息的构造方法 ==========

  /**
   * 创建请求异常（带消息格式化）
   *
   * @param code 错误码
   * @param message 错误消息模板
   * @param args 格式化参数
   */
  public RequestException(int code, String message, Object... args) {
    super(message, args);
    this.code = code;
    this.logging = true;
  }

  /**
   * 创建请求异常（带日志控制和消息格式化）
   */
  public RequestException(int code, boolean logging, String message, Object... args) {
    super(message, args);
    this.code = code;
    this.logging = logging;
  }

  // ========== 带根因异常的构造方法 ==========

  /**
   * 创建请求异常（带根因）
   */
  public RequestException(int code, Throwable cause) {
    super(cause);
    this.code = code;
    this.logging = true;
  }

  /**
   * 创建请求异常（带根因和日志控制）
   */
  public RequestException(int code, boolean logging, Throwable cause) {
    super(cause);
    this.code = code;
    this.logging = logging;
  }

  // ========== 带根因和消息的构造方法 ==========

  /**
   * 创建请求异常（带根因和消息）
   */
  public RequestException(int code, Throwable cause, String message) {
    super(cause, message);
    this.code = code;
    this.logging = true;
  }

  /**
   * 创建请求异常（带根因、消息和日志控制）
   */
  public RequestException(int code, boolean logging, Throwable cause, String message) {
    super(cause, message);
    this.code = code;
    this.logging = logging;
  }

  // ========== 完整参数的构造方法 ==========

  /**
   * 创建请求异常（带根因和格式化消息）
   */
  public RequestException(int code, Throwable cause, String message, Object... args) {
    super(cause, message, args);
    this.code = code;
    this.logging = true;
  }

  /**
   * 创建请求异常（完整参数）
   */
  public RequestException(int code, boolean logging, Throwable cause, String message, Object... args) {
    super(cause, message, args);
    this.code = code;
    this.logging = logging;
  }
}
