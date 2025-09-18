package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.annotation.MappingVersion;
import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementMethod;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import cn.labzen.web.ap.suggestion.RemoveSuggestion;
import cn.labzen.web.ap.suggestion.ReplaceSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import cn.labzen.web.defination.APIVersionCarrier;
import cn.labzen.web.runtime.annotation.APIVersion;
import com.google.common.collect.Lists;
import com.squareup.javapoet.TypeName;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

public final class MappingVersionEvaluator implements MethodErasableAnnotationEvaluator {

  private static final TypeName SUPPORTED = TypeName.get(MappingVersion.class);
  private static final String SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED);
  private static final TypeName MAPPING_TYPE = TypeName.get(RequestMapping.class);
  private static final String MAPPING_TYPE_NAME = Utils.getSimpleName(MAPPING_TYPE);
  private static final TypeName API_VERSION_TYPE = TypeName.get(APIVersion.class);

  @Override
  public boolean support(TypeName type) {
    return SUPPORTED.equals(type);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    List<Suggestion> suggestions = Lists.newArrayList(new RemoveSuggestion(SUPPORTED_NAME, ElementMethod.class));

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
    ElementAnnotation annotation = new ElementAnnotation(API_VERSION_TYPE, Map.of("value", version));
    return new AppendSuggestion(annotation, ElementMethod.class);
  }

  /**
   * 通过 Header Accept 版本控制
   */
  private Suggestion versionByHeader(Config config, String version) {
    String headerVersion = "application/vnd." + config.apiVersionHeaderVND() + "." + version + "+json";
    ElementAnnotation annotation = new ElementAnnotation(MAPPING_TYPE, Map.of("produces", new String[]{headerVersion}));
    return new ReplaceSuggestion(MAPPING_TYPE_NAME, annotation);
  }

  /**
   * 通过请求参数控制版本
   */
  private Suggestion versionByParameter(Config config, String version) {
    String paramVersion = config.apiVersionParameterName() + "=" + version;
    ElementAnnotation annotation = new ElementAnnotation(MAPPING_TYPE, Map.of("params", new String[]{paramVersion}));
    return new ReplaceSuggestion(MAPPING_TYPE_NAME, annotation);
  }
}
