package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.paging.internal.PageableSupport;
import jakarta.annotation.Nonnull;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

/**
 * 分页兼容参数解析器。
 * <p>
 * 处理以表单/query 方式绑定的 {@link Pageable} 资源参数：参数绑定与校验委托给 Spring 原生的
 * {@link ServletModelAttributeMethodProcessor} 完成，随后为绑定结果附加分页能力
 * （分页条件来自 query string，支持 {@code paging=1,20} 紧凑格式与 {@code pn}/{@code ps}/{@code od} 缩写）。
 * <p>
 * 仅接管「表单绑定」的参数：显式 {@link ModelAttribute}，或未声明任何 Spring 绑定注解的复杂类型。
 * 以 {@code @RequestBody} 方式绑定的参数不由此解析器处理，其分页能力由
 * {@code LabzenPageableRequestBodyAdvice} 在反序列化完成后附加。
 * <p>
 * 参数类型必须是具体类（能够被实例化并完成绑定），不能直接声明为 {@link Pageable} 接口。
 */
public class PageableCompatibleArgumentResolver implements HandlerMethodArgumentResolver {

  /**
   * Spring MVC 参数绑定注解所在包，用于判定参数是否为表单/query 来源
   */
  private static final String SPRING_BIND_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation.";

  /**
   * 表单绑定处理器，绑定、校验与 BindingResult 语义均使用 Spring 原生实现
   */
  private final ServletModelAttributeMethodProcessor formBinding = new ServletModelAttributeMethodProcessor(true);

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.isAssignableFrom(parameter.getParameterType()) && isFormBound(parameter);
  }

  @Override
  public Object resolveArgument(@Nonnull MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                @Nonnull NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
    Objects.requireNonNull(mavContainer, "PageableArgumentResolver requires ModelAndViewContainer");
    Objects.requireNonNull(binderFactory, "PageableArgumentResolver requires WebDataBinderFactory");

    Object bound = formBinding.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    return PageableSupport.attachPaging(parameter, bound, webRequest);
  }

  /**
   * 判断参数是否由表单/query 绑定
   * <p>
   * 显式 {@link ModelAttribute} 视为表单绑定；出现其他 Spring 绑定注解（如 {@code @RequestBody}、
   * {@code @RequestParam}、{@code @PathVariable}）则视为其他来源，不由本解析器接管。
   */
  private boolean isFormBound(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
      return true;
    }

    for (Annotation annotation : parametersAnnotations(parameter)) {
      if (annotation.annotationType().getName().startsWith(SPRING_BIND_ANNOTATION_PACKAGE)) {
        return false;
      }
    }
    return true;
  }

  private List<Annotation> parametersAnnotations(MethodParameter parameter) {
    return List.of(parameter.getParameterAnnotations());
  }
}
