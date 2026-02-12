package cn.labzen.web.exception;

import lombok.Getter;

@Deprecated
@Getter
public class TypeNotAvailableException extends ReflectiveOperationException {

  private final String fqcn;

  public TypeNotAvailableException(String fqcn, Throwable throwable) {
    super(throwable);
    this.fqcn = fqcn;
  }
}
