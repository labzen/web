package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementMethod;
import cn.labzen.web.apt.suggestion.AppendSuggestion;
import cn.labzen.web.apt.suggestion.ReplaceSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.labzen.web.apt.definition.TypeNames.ANNOTATION_API_VERSION;
import static cn.labzen.web.apt.definition.TypeNames.ANNOTATION_SPRING_REQUEST_MAPPING;

/**
 * Spring 请求映射注解评价器
 * <p>
 * 处理 Spring 的 @RequestMapping 及其派生注解（@GetMapping、@PostMapping 等），
 * 根据配置添加默认的 API 版本信息。
 */
public final class RequestMappingEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName apiVersionType;

  /**
   * 判断是否支持该注解类型
   *
   * @param type 注解类型
   * @return 是否支持（支持所有 Spring 请求映射注解）
   */
  @Override
  public boolean support(TypeName type) {
    return type instanceof ClassName cn && Utils.isRequestMappingAnnotation(cn);
  }

  /**
   * 初始化评价器，加载 @APIVersion 注解类型
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    apiVersionType = TypeName.get(context.elements().getTypeElement(ANNOTATION_API_VERSION).asType());
  }

  /**
   * 评价请求映射注解，添加默认版本控制
   *
   * @param config 处理器配置
   * @param type 注解类型
   * @param members 注解成员值
   * @return 代码生成建议列表
   */
  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    String carrier = config.apiVersionCarrier();
    if (Objects.equals(carrier, "DISABLE")) {
      return Collections.emptyList();
    }

    String version = config.apiVersionPrefix() + config.apiVersionBased();
    var annotation = switch (carrier) {
      case "URI" -> versionByURI(version);
      case "HEADER" -> versionByHeader(config, version, type);
      case "PARAMETER" -> versionByParameter(config, version, type);
      default -> throw new IllegalStateException("never happen");
    };

    return List.of(annotation);
  }

  /**
   * 通过 URI 方式添加版本注解
   *
   * @param version 版本号
   * @return 代码建议
   */
  private Suggestion versionByURI(String version) {
    ElementAnnotation annotation = new ElementAnnotation(apiVersionType, Map.of("value", version));
    return new AppendSuggestion(annotation, ElementMethod.class);
  }

  /**
   * 通过 Header Accept 方式设置版本
   *
   * @param config 处理器配置
   * @param version 版本号
   * @param type 注解类型
   * @return 代码建议
   */
  private Suggestion versionByHeader(Config config, String version, TypeName type) {
    String headerVersion = "application/vnd." + config.apiVersionHeaderVND() + "." + version + "+json";
    ElementAnnotation annotation = new ElementAnnotation(type, Map.of("produces", new String[]{headerVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }

  /**
   * 通过请求参数方式设置版本
   *
   * @param config 处理器配置
   * @param version 版本号
   * @param type 注解类型
   * @return 代码建议
   */
  private Suggestion versionByParameter(Config config, String version, TypeName type) {
    String paramVersion = config.apiVersionParameterName() + "=" + version;
    ElementAnnotation annotation = new ElementAnnotation(type, Map.of("params", new String[]{paramVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }
}
