package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
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
 * Pageable 参数解析器
 * <p>
 * 负责将 HTTP 请求参数绑定到实现 {@link Pageable} 接口的方法参数上。
 * 核心功能：
 * <ul>
 *   <li>支持两种分页参数格式：紧凑格式（paging=1,20）和普通格式（pageNumber=1, pageSize=20）</li>
 *   <li>支持排序条件解析</li>
 *   <li>使用 ByteBuddy 动态代理实现分页数据的懒加载</li>
 * </ul>
 */
public class PageableCompatibleArgumentResolver implements HandlerMethodArgumentResolver {

  /**
   * 判断是否支持该参数类型
   */
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.isAssignableFrom(parameter.getParameterType());
  }

  /**
   * 解析 Pageable 参数
   * <p>
   * 核心流程：
   * <ul>
   *   <li>1. 绑定 Bean 参数值（处理 @ModelAttribute 注解的参数）</li>
   *   <li>2. 解析分页条件（页码、每页数量、排序）</li>
   *   <li>3. 使用动态代理创建可访问分页数据的对象</li>
   * </ul>
   */
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
   * 绑定请求参数到 Bean 对象
   * <p>
   * 包含完整的 Spring 数据绑定和验证流程。
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
