package cn.labzen.web.apt.evaluate.annotation;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementMethod;
import cn.labzen.web.apt.suggestion.AppendSuggestion;
import cn.labzen.web.apt.suggestion.RemoveSuggestion;
import cn.labzen.web.apt.suggestion.ReplaceSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.google.common.collect.Lists;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.labzen.web.apt.definition.TypeNames.*;

/**
 * @MappingVersion 注解评价器
 * <p>
 * 处理接口方法上的 @MappingVersion 注解，根据配置的版本携带方式生成相应的代码建议：
 * <ul>
 *   <li>URI - 添加 @APIVersion 注解</li>
 *   <li>HEADER - 修改 @RequestMapping 的 produces 属性</li>
 *   <li>PARAMETER - 修改 @RequestMapping 的 params 属性</li>
 * </ul>
 */
public final class MappingVersionEvaluator implements MethodAnnotationErasableEvaluator {

  private TypeName supportedAnnotationType;
  private TypeName apiVersionType;
  private TypeName requestMappingType;

  /**
   * 初始化评价器，加载必要的类型信息
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    supportedAnnotationType = TypeName.get(context.elements().getTypeElement(APT_ANNOTATION_MAPPING_VERSION).asType());
    apiVersionType = TypeName.get(context.elements().getTypeElement(ANNOTATION_API_VERSION).asType());
    requestMappingType = TypeName.get(context.elements().getTypeElement(ANNOTATION_SPRING_REQUEST_MAPPING).asType());
  }

  /**
   * 判断是否支持该注解类型
   *
   * @param type 注解类型
   * @return 是否支持
   */
  @Override
  public boolean support(TypeName type) {
    return supportedAnnotationType.equals(type);
  }

  /**
   * 评价 @MappingVersion 注解，生成版本控制建议
   *
   * @param config 处理器配置
   * @param type 注解类型
   * @param members 注解成员值
   * @return 代码生成建议列表
   */
  @Override
  public List<? extends Suggestion> evaluate(Config config, TypeName type, Map<String, Object> members) {
    List<Suggestion> suggestions = Lists.newArrayList(new RemoveSuggestion(Utils.getSimpleName(supportedAnnotationType), ElementMethod.class));

    String carrier = config.apiVersionCarrier();
    if (Objects.equals(carrier, "DISABLE")) {
      return suggestions;
    }

    String version = config.apiVersionPrefix() + members.values().stream().toList().getFirst();

    var annotation = switch (carrier) {
      case "URI" -> versionByURI(version);
      case "HEADER" -> versionByHeader(config, version);
      case "PARAMETER" -> versionByParameter(config, version);
      default -> throw new IllegalStateException("never happen");
    };
    suggestions.add(annotation);

    return suggestions;
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
   * @return 代码建议
   */
  private Suggestion versionByHeader(Config config, String version) {
    String headerVersion = "application/vnd." + config.apiVersionHeaderVND() + "." + version + "+json";
    ElementAnnotation annotation = new ElementAnnotation(requestMappingType, Map.of("produces", new String[]{headerVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }

  /**
   * 通过请求参数方式设置版本
   *
   * @param config 处理器配置
   * @param version 版本号
   * @return 代码建议
   */
  private Suggestion versionByParameter(Config config, String version) {
    String paramVersion = config.apiVersionParameterName() + "=" + version;
    ElementAnnotation annotation = new ElementAnnotation(requestMappingType, Map.of("params", new String[]{paramVersion}));
    return new ReplaceSuggestion(ANNOTATION_SPRING_REQUEST_MAPPING, annotation);
  }
}
