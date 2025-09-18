package cn.labzen.web.ap.internal;

import com.squareup.javapoet.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Utils {

  private static final Set<String> REQUEST_MAPPING_ANNOTATIONS = Set.of(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.PatchMapping");

  private Utils() {
  }

  public static boolean isRequestMappingAnnotation(ClassName className) {
    return REQUEST_MAPPING_ANNOTATIONS.contains(className.canonicalName());
  }

  public static Map<String, Object> readAnnotationMembers(AnnotationMirror annotation) {
    return annotation.getElementValues().entrySet().stream()
      .collect(Collectors.toMap(
        entry -> entry.getKey().getSimpleName().toString(),
        entry -> {
          Object value = entry.getValue().getValue();
          if (value instanceof List<?> list) {
            return list.stream()
              .map(item -> ((AnnotationValue) item).getValue()).collect(Collectors.toList());
          }

          return value;
        }));
  }

  public static ClassName classOf(Element type) {
    return ClassName.get((TypeElement) type);
  }

  public static TypeName typeOf(TypeMirror type) {
    return TypeName.get(type);
  }

  @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
  public static String getSimpleName(TypeName type) {
    return switch (type) {
      case ClassName className -> className.simpleName();
      case ArrayTypeName arrayTypeName -> getSimpleName(arrayTypeName.componentType) + "[]";
      case ParameterizedTypeName parameterizedTypeName -> {
        String raw = getSimpleName(parameterizedTypeName.rawType);
        String typeArgs = parameterizedTypeName.typeArguments.stream().map(Utils::getSimpleName).collect(Collectors.joining(","));
        yield raw + "<" + typeArgs + ">";
      }
      case TypeVariableName typeVariableName -> typeVariableName.name;
      case WildcardTypeName wildcardTypeName -> {
        var out = wildcardTypeName.upperBounds;
        var in = wildcardTypeName.lowerBounds;
        if (!in.isEmpty()) {
          yield "? super " + getSimpleName(in.get(0));
        } else if (!out.isEmpty() && !out.get(0).equals(ClassName.OBJECT)) {
          yield "? extends " + getSimpleName(out.get(0));
        } else {
          yield "?";
        }
      }
      default -> type.toString();
    };
  }
}
