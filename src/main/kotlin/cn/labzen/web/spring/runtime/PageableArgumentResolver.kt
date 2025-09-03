package cn.labzen.web.spring.runtime

import cn.labzen.web.paging.Pageable
import cn.labzen.web.paging.internal.PageableDelegationProcessor
import cn.labzen.web.paging.internal.PageableResolver
import org.springframework.beans.BeanUtils
import org.springframework.core.MethodParameter
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.springframework.validation.annotation.ValidationAnnotationUtils
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor
import org.springframework.web.method.annotation.ModelFactory
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder
import javax.servlet.ServletRequest

/**
 * 负责在查询请求时，对实现 [Pageable] 接口的 Resource Bean 参数的数据绑定处理
 */
class PageableArgumentResolver : HandlerMethodArgumentResolver {

  override fun supportsParameter(parameter: MethodParameter): Boolean =
    Pageable::class.java.isAssignableFrom(parameter.parameterType)

  override fun resolveArgument(
    parameter: MethodParameter,
    mavContainer: ModelAndViewContainer?,
    webRequest: NativeWebRequest,
    binderFactory: WebDataBinderFactory?
  ): Any? {
    assert(mavContainer != null) { "PageableArgumentResolver requires ModelAndViewContainer" }
    assert(binderFactory != null) { "PageableArgumentResolver requires WebDataBinderFactory" }

    // 1. 先绑定Bean的参数值
    val attribute = bindAttribute(parameter, mavContainer!!, webRequest, binderFactory!!)

    // 2. 先尝试读取分析分页相关参数，如果没有，返回默认分页条件
    val resolvedPaging = PageableResolver.resolve(webRequest)

    // 3. 对数据绑定好的参数实例，进行代理，在代理中提供解析好的分页数据
    return PageableDelegationProcessor.delegate(parameter, attribute, resolvedPaging)
  }

  /**
   * 参数解析及绑定逻辑，与 Spring 官方 [ModelAttributeMethodProcessor] 中的实现代码，不能说一样吧，反正我是抄的
   */
  private fun bindAttribute(
    parameter: MethodParameter,
    mavContainer: ModelAndViewContainer,
    webRequest: NativeWebRequest,
    binderFactory: WebDataBinderFactory
  ): Any {
    val name = ModelFactory.getNameForParameter(parameter)
    parameter.getParameterAnnotation(ModelAttribute::class.java)?.run {
      mavContainer.setBinding(name, this.binding)
    }

    val clazz = parameter.parameterType
    var attribute = if (mavContainer.containsAttribute(name)) {
      mavContainer.model[name]!!
    } else {
      BeanUtils.instantiateClass(clazz)
    }

    val binder = binderFactory.createBinder(webRequest, attribute, name)
    if (binder.target != null) {
      if (!mavContainer.isBindingDisabled(name)) {
//        (binder as WebRequestDataBinder).bind(webRequest)
        (binder as ExtendedServletRequestDataBinder).bind(webRequest.nativeRequest as ServletRequest)
      }
      validateIfApplicable(binder, parameter)
      if (binder.bindingResult.hasErrors() && isBindExceptionRequired(parameter)) {
        throw BindException(binder.bindingResult)
      }
    }

    // Value type adaptation, also covering java.util.Optional
    if (!parameter.parameterType.isInstance(attribute)) {
      attribute = binder.convertIfNecessary(binder.target, parameter.parameterType, parameter)
    }
    val bindingResult = binder.bindingResult

    // Add resolved attribute and BindingResult at the end of the model
    val bindingResultModel = bindingResult.model
    mavContainer.removeAttributes(bindingResultModel)
    mavContainer.addAllAttributes(bindingResultModel)

    return attribute
  }

  private fun isBindExceptionRequired(parameter: MethodParameter): Boolean {
    val i = parameter.parameterIndex
    val paramTypes = parameter.executable.parameterTypes
    val hasBindingResult = (paramTypes.size > (i + 1) && Errors::class.java.isAssignableFrom(
      paramTypes[i + 1]
    ))
    return !hasBindingResult
  }

  private fun validateIfApplicable(binder: WebDataBinder, parameter: MethodParameter) {
    for (ann in parameter.parameterAnnotations) {
      val validationHints = ValidationAnnotationUtils.determineValidationHints(ann)
      if (validationHints != null) {
        binder.validate(*validationHints)
        break
      }
    }
  }
}
