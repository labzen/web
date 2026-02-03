package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementMethod;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.ReplaceSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import cn.labzen.web.defination.APIVersionCarrier;
import com.google.common.collect.Lists;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import static cn.labzen.web.ap.definition.TypeNames.*;

public final class MappingVersionEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;
  private TypeName apiVersionType;
  private TypeName requestMappingType;

  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(AP_ANNOTATION_MAPPING_VERSION).asType());
    apiVersionType = TypeName.get(context.elements().getTypeElement(ANNOTATION_API_VERSION).asType());
    requestMappingType = TypeName.get(context.elements().getTypeElement(ANNOTATION_SPRING_REQUEST_MAPPING).asType());
  }

  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    List<Suggestion> suggestions = Lists.newArrayList(new RemoveSuggestion(Utils.getSimpleName(supportedAnnotationType), ElementMethod.class));

    APIVersionCarrier carrier = config.apiVersionCarrier();
    if (carrier == APIVersionCarrier.DISABLE) {
      return suggestions;
    }

    String version = config.apiVersionPrefix() + members.values().stream().toList().getFirst();

    var annotation = switch (carrier) {
      case URI -> versionByURI(version);
      case HEADER -> versionByHeader(config, version);
      case PARAMETER -> versionByParameter(config, version);
      default -> throw new IllegalStateException("never happen");
    };
    suggestions.add(annotation);

    return suggestions;
  }

  /**
   * 通过 URI 版本控制
   */
  private Suggestion versionByURI(String version) {
    ElementAnnotation annotation = new ElementAnnotation(apiVersionType, Map.of("value", version));
    return new AppendSuggestion(annotation, ElementMethod.class);
  }

  /**
   * 通过 Header Accept 版本控制
   */
  private Suggestion versionByHeader(Config config, String version) {
    String headerVersion = "application/vnd." + config.apiVersionHeaderVND() + "." + version + "+json";
    ElementAnnotation annotation = new ElementAnnotation(requestMappingType, Map.of("produces", new String[]{headerVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }

  /**
   * 通过请求参数控制版本
   */
  private Suggestion versionByParameter(Config config, String version) {
    String paramVersion = config.apiVersionParameterName() + "=" + version;
    ElementAnnotation annotation = new ElementAnnotation(requestMappingType, Map.of("params", new String[]{paramVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }
}
