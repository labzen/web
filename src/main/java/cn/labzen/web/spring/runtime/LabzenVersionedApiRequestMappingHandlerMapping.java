package cn.labzen.web.spring.runtime;

import cn.labzen.web.defination.APIVersionCarrier;
import cn.labzen.web.runtime.annotation.APIVersion;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Nonnull;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 自定义 {@link RequestMappingHandlerMapping} ，以适配通过 {@link APIVersionCarrier#URI} 方式控制API版本的请求映射
 * <p>
 * 仅当 `labzen.yml` 的配置项 `api-version.carrier = URI` 时生效
 */
public class LabzenVersionedApiRequestMappingHandlerMapping
  extends RequestMappingHandlerMapping implements EmbeddedValueResolverAware {

  private StringValueResolver embeddedValueResolver;

  @Override
  public void setEmbeddedValueResolver(@Nonnull StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
    super.setEmbeddedValueResolver(resolver);
  }

  @Override
  protected RequestMappingInfo getMappingForMethod(@Nonnull Method method, @Nonnull Class<?> handlerType) {
    RequestMappingInfo info = createRequestMappingInfo(method);
    if (info == null) {
      return null;
    }

    RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
    if (typeInfo != null) {
      info = typeInfo.combine(info);
    }

    // 从这里开始才是重点
    var versionedMappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, APIVersion.class);
    // 如果包含APIVersion注解，才会在映射路径中加入版本标识
    if (versionedMappingAnnotation != null) {
      String version = versionedMappingAnnotation.value();
      var versionInfo = RequestMappingInfo.paths(version)
        .options(this.getBuilderConfiguration())
        .build();
      info = versionInfo.combine(info);
    }

    String prefix = getPathPrefix(handlerType);
    if (prefix != null) {
      var prefixInfo = RequestMappingInfo.paths(prefix)
        .options(this.getBuilderConfiguration())
        .build();
      info = prefixInfo.combine(info);
    }
    return info;
  }

  private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
    var requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    var condition = (element instanceof Class<?> clazz)
      ? getCustomTypeCondition(clazz)
      : getCustomMethodCondition((Method) element);

    return (requestMapping != null)
      ? super.createRequestMappingInfo(requestMapping, condition)
      : null;
  }

  private String getPathPrefix(Class<?> handlerType) {
    // 假设 pathPrefixes 是一个 Map<String, Predicate<Class<?>>> 类型的字段
    // 这里需要根据实际实现调整
    Map<String, Predicate<Class<?>>> pathPrefixes = super.getPathPrefixes();
    for (var entry : pathPrefixes.entrySet()) {
      if (entry.getValue().test(handlerType)) {
        String prefix = entry.getKey();
        if (this.embeddedValueResolver != null) {
          prefix = this.embeddedValueResolver.resolveStringValue(prefix);
        }
        return prefix;
      }
    }
    return null;
  }
}