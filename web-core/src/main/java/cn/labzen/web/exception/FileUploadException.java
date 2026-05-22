package cn.labzen.web.exception;

public class FileUploadException extends RequestException {

  public FileUploadException(int code, String message) {
    super(code, message);
  }

  public FileUploadException(int code, String message, Object... args) {
    super(code, message, args);
  }

  public FileUploadException(int code, Throwable cause) {
    super(code, cause);
  }

  public FileUploadException(int code, Throwable cause, String message) {
    super(code, cause, message);
  }

  public FileUploadException(int code, Throwable cause, String message, Object... args) {
    super(code, cause, message, args);
  }
}
