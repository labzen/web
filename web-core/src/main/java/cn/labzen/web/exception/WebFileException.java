package cn.labzen.web.exception;

public class WebFileException extends RequestException {


  public WebFileException(int code, Throwable cause) {
    super(code, cause);
  }

  public WebFileException(int code, String message) {
    super(code, message);
  }

  public WebFileException(int code, Throwable cause, String message) {
    super(code, cause, message);
  }
}
