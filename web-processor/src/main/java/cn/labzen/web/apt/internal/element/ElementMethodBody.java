package cn.labzen.web.apt.internal.element;

import lombok.Getter;

import java.util.List;

@Getter
public final class ElementMethodBody implements Element {

  private final String fieldName;
  private final String invokeMethodName;
  private final List<String> parameterNames;

  public ElementMethodBody(String fieldName, String invokeMethodName, List<String> parameterNames) {
    this.fieldName = fieldName;
    this.invokeMethodName = invokeMethodName;
    this.parameterNames = parameterNames;
  }

  @Override
  public String keyword() {
    return "{}";
  }
}
