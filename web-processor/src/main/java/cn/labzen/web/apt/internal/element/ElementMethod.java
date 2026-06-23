package cn.labzen.web.apt.internal.element;

import cn.labzen.web.apt.internal.Utils;
import com.squareup.javapoet.TypeName;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public final class ElementMethod implements Element {

  private final String name;
  private final TypeName returnType;

  private final LinkedHashSet<ElementParameter> parameters = new LinkedHashSet<>();
  private final LinkedHashSet<ElementAnnotation> annotations = new LinkedHashSet<>();
  @Setter
  private ElementMethodBody body;

  public ElementMethod(String name, TypeName returnType) {
    this.name = name;
    this.returnType = returnType;
  }

  @Override
  public String keyword() {
    String parameters = this.parameters.stream().sorted(Comparator.comparingInt(ElementParameter::getIndex)).map(ElementParameter::keyword).collect(Collectors.joining(", "));
    return Utils.getSimpleName(returnType) + " " + name + "(" + parameters + ")";
  }

  @Override
  public String toString() {
    return keyword();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ElementMethod that = (ElementMethod) o;
    return Objects.equals(keyword(), that.keyword());
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyword());
  }
}
