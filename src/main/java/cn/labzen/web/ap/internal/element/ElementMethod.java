package cn.labzen.web.ap.internal.element;

import cn.labzen.web.ap.internal.Utils;
import com.squareup.javapoet.TypeName;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.LinkedHashSet;
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
}
