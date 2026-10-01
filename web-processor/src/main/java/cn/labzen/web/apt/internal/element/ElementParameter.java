package cn.labzen.web.apt.internal.element;

import cn.labzen.web.apt.internal.Utils;
import com.squareup.javapoet.TypeName;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Objects;

@Getter
public final class ElementParameter implements Element {

  private final int index;
  private final String name;
  private final TypeName type;
  private final LinkedHashSet<ElementAnnotation> annotations;

  /**
   * 空请求体兜底标记。为 true 时，生成的实现类会在调用业务方法前，将 null 的资源入参替换为该类型的空实例
   */
  @Setter
  private boolean emptyBodyFallback;

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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElementParameter parameter = (ElementParameter) o;
    return index == parameter.index && Objects.equals(type, parameter.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, type);
  }
}
