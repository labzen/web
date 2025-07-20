package cn.labzen.web.paging.internal

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import org.springframework.core.MethodParameter

object PageableDelegationProcessor {

  fun delegate(parameter: MethodParameter, attribute: Any, resolvedPaging: Paging): Any? {
    val clazz = parameter.parameterType
    val pageableMethodNames =
      ElementMatchers.namedOneOf<MethodDescription>("isUnpaged", "pageNumber", "pageSize", "orders", "convertTo")
    return ByteBuddy()
      .subclass(clazz)
      .method(pageableMethodNames)
      .intercept(MethodDelegation.to(PageableValuesInterceptor(resolvedPaging)))
      .method(ElementMatchers.not(pageableMethodNames))
      .intercept(MethodDelegation.to(PageableBeanAttributesInterceptor(attribute)))
      .make()
      .load(clazz.classLoader)
      .loaded
      .getDeclaredConstructor()
      .newInstance()
  }
}