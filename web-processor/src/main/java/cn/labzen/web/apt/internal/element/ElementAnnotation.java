package cn.labzen.web.apt.internal.element;

import cn.labzen.web.apt.internal.Utils;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.Getter;

import java.util.Map;

import static cn.labzen.web.apt.definition.TypeNames.ANNOTATION_SPRING_REQUEST_MAPPING;

@Getter
public final class ElementAnnotation implements Element {

  private final TypeName type;
  private final Map<String, Object> members;

  public ElementAnnotation(TypeName type) {
    this(type, Maps.newHashMap());
  }

  public ElementAnnotation(TypeName type, Map<String, Object> members) {
    this.type = type;
    this.members = members;
  }

  @Override
  public String keyword() {
    if (Utils.isRequestMappingAnnotation((ClassName) type)) {
      return ANNOTATION_SPRING_REQUEST_MAPPING;
    }

    return Utils.getSimpleName(type);
  }

  @Override
  public String toString() {
    return keyword();
  }
}
