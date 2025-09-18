package cn.labzen.web.ap.evaluate.annotation;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementMethod;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import cn.labzen.web.ap.suggestion.ReplaceSuggestion;
import cn.labzen.web.ap.suggestion.Suggestion;
import cn.labzen.web.defination.APIVersionCarrier;
import cn.labzen.web.runtime.annotation.APIVersion;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RequestMappingEvaluator implements MethodErasableAnnotationEvaluator {

  private static final TypeName SUPPORTED = TypeName.get(RequestMapping.class);
  private static final String SUPPORTED_NAME = Utils.getSimpleName(SUPPORTED);
  private static final TypeName API_VERSION_TYPE = TypeName.get(APIVersion.class);

  @Override
  public boolean support(TypeName type) {
    return type instanceof ClassName cn && Utils.isRequestMappingAnnotation(cn);
  }

  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    APIVersionCarrier carrier = config.apiVersionCarrier();
    if (carrier == APIVersionCarrier.DISABLE) {
      return Collections.emptyList();
    }

    String version = config.apiVersionPrefix() + config.apiVersionBased();
    var annotation = switch (carrier) {
      case URI -> versionByURI(version);
      case HEADER -> versionByHeader(config, version, type);
      case PARAMETER -> versionByParameter(config, version, type);
      default -> throw new IllegalStateException("never happen");
    };

    return List.of(annotation);
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
  private Suggestion versionByHeader(Config config, String version, TypeName type) {
    String headerVersion = "application/vnd." + config.apiVersionHeaderVND() + "." + version + "+json";
    ElementAnnotation annotation = new ElementAnnotation(type, Map.of("produces", new String[]{headerVersion}));
    return new ReplaceSuggestion(SUPPORTED_NAME, annotation);
  }

  /**
   * 通过请求参数控制版本
   */
  private Suggestion versionByParameter(Config config, String version, TypeName type) {
    String paramVersion = config.apiVersionParameterName() + "=" + version;
    ElementAnnotation annotation = new ElementAnnotation(type, Map.of("params", new String[]{paramVersion}));
    return new ReplaceSuggestion(SUPPORTED_NAME, annotation);
  }
}
