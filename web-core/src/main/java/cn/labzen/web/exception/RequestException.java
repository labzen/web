package cn.labzen.web.exception;

import cn.labzen.meta.exception.LabzenRuntimeException;

public class RequestException extends LabzenRuntimeException {

  private final int code;
  private final boolean logging;

  public RequestException(boolean logging, String message) {
    super(message);
    this.code = 500;
    this.logging = logging;
  }

  public RequestException(int code, String message) {
    super(message);
    this.code = code;
    this.logging = true;
  }

  public RequestException(int code, boolean logging, String message) {
    super(message);
    this.code = code;
    this.logging = logging;
  }

  public RequestException(boolean logging, String message, Object... args) {
    super(message, args);
    this.code = 500;
    this.logging = logging;
  }

  public RequestException(int code, String message, Object... args) {
    super(message, args);
    this.code = code;
    this.logging = true;
  }

  public RequestException(int code, boolean logging, String message, Object... args) {
    super(message, args);
    this.code = code;
    this.logging = logging;
  }

  public RequestException(boolean logging, Throwable cause) {
    super(cause);
    this.code = 500;
    this.logging = true;
  }

  public RequestException(int code, Throwable cause) {
    super(cause);
    this.code = code;
    this.logging = true;
  }

  public RequestException(int code, boolean logging, Throwable cause) {
    super(cause);
    this.code = code;
    this.logging = logging;
  }

  public RequestException(boolean logging, Throwable cause, String message) {
    super(cause, message);
    this.code = 500;
    this.logging = logging;
  }

  public RequestException(int code, Throwable cause, String message) {
    super(cause, message);
    this.code = code;
    this.logging = true;
  }

  public RequestException(int code, boolean logging, Throwable cause, String message) {
    super(cause, message);
    this.code = code;
    this.logging = logging;
  }

  public RequestException(boolean logging, Throwable cause, String message, Object... args) {
    super(cause, message, args);
    this.code = 500;
    this.logging = logging;
  }

  public RequestException(int code, Throwable cause, String message, Object... args) {
    super(cause, message, args);
    this.code = code;
    this.logging = true;
  }

  public RequestException(int code, boolean logging, Throwable cause, String message, Object... args) {
    super(cause, message, args);
    this.code = code;
    this.logging = logging;
  }

  public int getCode() {
    return code;
  }

  public boolean isLogging() {
    return logging;
  }
}
