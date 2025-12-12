package cn.labzen.web.spring.runtime;

import cn.labzen.web.paging.Pageable;
import cn.labzen.web.paging.internal.PageableDelegator;
import cn.labzen.web.paging.internal.PageableResolver;
import jakarta.servlet.ServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 负责在查询请求时，对实现 {@link Pageable} 接口的 Resource Bean 参数的数据绑定处理
 */
public class PageableCompatibleArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(@Nonnull MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                @Nonnull NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
    Objects.requireNonNull(mavContainer, "PageableArgumentResolver requires ModelAndViewContainer");
    Objects.requireNonNull(binderFactory, "PageableArgumentResolver requires WebDataBinderFactory");

    try {
      // 1. 先绑定Bean的参数值
      Object attribute = bindAttribute(parameter, mavContainer, webRequest, binderFactory);

      // 2. 先尝试读取分析分页相关参数，如果没有，返回默认分页条件
      var resolvedPaging = PageableResolver.resolve(webRequest);

      // 3. 对数据绑定好的参数实例，进行代理，在代理中提供解析好的分页数据
      return PageableDelegator.delegate(parameter, attribute, resolvedPaging);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 参数解析及绑定逻辑
   */
  private Object bindAttribute(MethodParameter parameter, ModelAndViewContainer mavContainer,
                               NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String name = ModelFactory.getNameForParameter(parameter);
    var modelAttribute = parameter.getParameterAnnotation(ModelAttribute.class);
    if (modelAttribute != null) {
      mavContainer.setBinding(name, modelAttribute.binding());
    }

    Class<?> clazz = parameter.getParameterType();
    Object attribute = mavContainer.containsAttribute(name)
      ? mavContainer.getModel().get(name)
      : BeanUtils.instantiateClass(clazz);

    WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
    if (binder.getTarget() != null) {
      if (!mavContainer.isBindingDisabled(name)) {
        ((ExtendedServletRequestDataBinder) binder).bind(Objects.requireNonNull(webRequest.getNativeRequest(ServletRequest.class)));
      }
      validateIfApplicable(binder, parameter);
      if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(parameter)) {
        throw new BindException(binder.getBindingResult());
      }
    }

    // Value type adaptation
    if (!clazz.isInstance(attribute)) {
      attribute = binder.convertIfNecessary(binder.getTarget(), clazz, parameter);
    }
    var bindingResult = binder.getBindingResult();

    // Add resolved attribute and BindingResult at the end of the model
    var bindingResultModel = bindingResult.getModel();
    mavContainer.removeAttributes(bindingResultModel);
    mavContainer.addAllAttributes(bindingResultModel);

    return attribute;
  }

  private boolean isBindExceptionRequired(MethodParameter parameter) {
    int i = parameter.getParameterIndex();
    Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
    boolean hasBindingResult = (paramTypes.length > (i + 1) &&
      Errors.class.isAssignableFrom(paramTypes[i + 1]));
    return !hasBindingResult;
  }

  private void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
    for (var ann : parameter.getParameterAnnotations()) {
      var validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
      if (validationHints != null) {
        binder.validate(validationHints);
        break;
      }
    }
  }
}
