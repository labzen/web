package cn.labzen.web.ap.internal.element;

import cn.labzen.web.ap.internal.Utils;
import com.squareup.javapoet.TypeName;
import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.Objects;

@Getter
public final class ElementParameter implements Element {

  private final int index;
  private final String name;
  private final TypeName type;
  private final LinkedHashSet<ElementAnnotation> annotations;

  public ElementParameter(int index, String name, TypeName type, LinkedHashSet<ElementAnnotation> annotations) {
    this.index = index;
    this.name = name;
    this.type = type;
    this.annotations = annotations;
  }

  @Override
  public String keyword() {
    return Utils.getSimpleName(type);
  }

  @Override
  public String toString() {
    return keyword();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ElementParameter parameter = (ElementParameter) o;
    return index == parameter.index && Objects.equals(type, parameter.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, type);
  }
}
