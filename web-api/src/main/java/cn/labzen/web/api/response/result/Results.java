package cn.labzen.web.api.response.result;

import cn.labzen.web.api.definition.HttpStatusExt;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.OutputStream;

/**
 * 提供创建 {@link ValueResult} 对象的便捷工厂方法
 */
public final class Results {

  private Results() {
  }

  /**
   * 创建成功响应（状态码 200，无数据，无消息）
   *
   * @return 成功响应对象
   */
  public static Result success() {
    return new ValueResult(200, null, null);
  }

  /**
   * 创建失败响应（状态码 500，无数据，无消息）
   *
   * @return 失败响应对象
   */
  public static Result failure() {
    return new ValueResult(500, null, null);
  }

  /**
   * 创建带消息的失败响应（状态码 500）
   *
   * @param message 错误消息
   * @return 带消息的失败响应对象
   */
  public static Result failure(String message) {
    return new ValueResult(500, null, message);
  }

  /* ======================== with status ======================== */

  /**
   * 创建指定状态码的响应（无数据，无消息）
   *
   * @param code HTTP 状态码
   * @return 指定状态码的响应对象
   */
  public static Result status(int code) {
    return new ValueResult(code, null, null);
  }

  /**
   * 创建指定 Spring HttpStatus 的响应（无数据，无消息）
   *
   * @param status Spring HTTP 状态枚举
   * @return 指定状态码的响应对象
   */
  public static Result status(HttpStatus status) {
    return new ValueResult(status.value(), null, null);
  }

  /**
   * 创建指定扩展 HttpStatus 的响应（无数据，无消息）
   *
   * @param status 扩展 HTTP 状态枚举
   * @return 指定状态码的响应对象
   */
  public static Result status(HttpStatusExt status) {
    return new ValueResult(status.code(), null, null);
  }

  /* ======================== with file ======================== */

  /**
   * 创建文件响应
   *
   * @param file 文件对象，将使用改文件名作为下载文件名称
   * @return 带文件的响应对象
   */
  public static Result file(File file) {
    return new FileResult(file);
  }

  /**
   * 创建带文件名的文件响应
   *
   * @param file     文件对象
   * @param filename 文件名
   * @return 带文件和文件名的响应对象
   */
  public static Result file(File file, String filename) {
    return new FileResult(filename, file);
  }

  /* ======================== with value ======================== */

  /**
   * 创建带数据的成功响应（状态码 200）
   *
   * @param value 响应数据
   * @return 带数据的响应对象
   */
  public static Result with(Object value) {
    return new ValueResult(200, value, null);
  }

  /**
   * 创建带状态码和数据的响应
   *
   * @param code  HTTP 状态码
   * @param value 响应数据
   * @return 带状态码和数据的响应对象
   */
  public static Result with(int code, Object value) {
    return new ValueResult(code, value, null);
  }

  /**
   * 创建带 Spring HttpStatus 和数据的响应
   *
   * @param status Spring HTTP 状态枚举
   * @param value  响应数据
   * @return 带状态和数据的响应对象
   */
  public static Result with(HttpStatus status, Object value) {
    return new ValueResult(status.value(), value, null);
  }

  /**
   * 创建带扩展 HttpStatus 和数据的响应
   *
   * @param status 扩展 HTTP 状态枚举
   * @param value  响应数据
   * @return 带状态和数据的响应对象
   */
  public static Result with(HttpStatusExt status, Object value) {
    return new ValueResult(status.code(), value, null);
  }

  /**
   * 创建带状态码、数据和消息的响应
   *
   * @param code    HTTP 状态码
   * @param value   响应数据
   * @param message 响应消息
   * @return 带状态码、数据和消息的响应对象
   */
  public static Result with(int code, Object value, String message) {
    return new ValueResult(code, value, message);
  }

  /**
   * 创建带 Spring HttpStatus、数据和消息的响应
   *
   * @param status  Spring HTTP 状态枚举
   * @param value   响应数据
   * @param message 响应消息
   * @return 带状态、数据和消息的响应对象
   */
  public static Result with(HttpStatus status, Object value, String message) {
    return new ValueResult(status.value(), value, message);
  }

  /**
   * 创建带扩展 HttpStatus、数据和消息的响应
   *
   * @param status  扩展 HTTP 状态枚举
   * @param value   响应数据
   * @param message 响应消息
   * @return 带状态、数据和消息的响应对象
   */
  public static Result with(HttpStatusExt status, Object value, String message) {
    return new ValueResult(status.code(), value, message);
  }

  /* ======================== with message ======================== */

  /**
   * 创建带消息的成功响应（状态码 200，无数据）
   *
   * @param message 响应消息
   * @return 带消息的响应对象
   */
  public static Result message(String message) {
    return new ValueResult(200, null, message);
  }

  /**
   * 创建带状态码和消息的响应（无数据）
   *
   * @param code    HTTP 状态码
   * @param message 响应消息
   * @return 带状态码和消息的响应对象
   */
  public static Result message(int code, String message) {
    return new ValueResult(code, null, message);
  }

  /**
   * 创建带 Spring HttpStatus 和消息的响应（无数据）
   *
   * @param status  Spring HTTP 状态枚举
   * @param message 响应消息
   * @return 带状态和消息的响应对象
   */
  public static Result message(HttpStatus status, String message) {
    return new ValueResult(status.value(), null, message);
  }

  /**
   * 创建带扩展 HttpStatus 和消息的响应（无数据）
   *
   * @param status  扩展 HTTP 状态枚举
   * @param message 响应消息
   * @return 带状态和消息的响应对象
   */
  public static Result message(HttpStatusExt status, String message) {
    return new ValueResult(status.code(), null, message);
  }

}
