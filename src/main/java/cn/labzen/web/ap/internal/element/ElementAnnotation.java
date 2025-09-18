package cn.labzen.web.ap.internal.element;

import cn.labzen.web.ap.internal.Utils;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Getter
public final class ElementAnnotation implements Element {

  private static final String MAPPING_TYPE_NAME = Utils.getSimpleName(ClassName.get(RequestMapping.class));

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
      return MAPPING_TYPE_NAME;
    }

    return Utils.getSimpleName(type);
  }

  @Override
  public String toString() {
    return keyword();
  }
}
