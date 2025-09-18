package cn.labzen.web.ap.internal.element;

import cn.labzen.web.ap.internal.Utils;
import com.squareup.javapoet.TypeName;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class ElementField implements Element {

  private final String name;
  private final TypeName type;
  private final List<ElementAnnotation> annotations;

  public ElementField(String name, TypeName type, List<ElementAnnotation> annotations) {
    this.name = name;
    this.type = type;
    this.annotations = annotations;
  }

  @Override
  public String keyword() {
    return Utils.getSimpleName(type) + " " + name;
  }

  @Override
  public String toString() {
    return keyword();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ElementField that = (ElementField) o;
    return Objects.equals(name, that.name) && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }
}
