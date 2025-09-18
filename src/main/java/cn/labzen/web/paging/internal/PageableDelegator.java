package cn.labzen.web.paging.internal;

import cn.labzen.web.paging.convert.PageConverterHolder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.core.MethodParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PageableDelegator {

  private PageableDelegator() {
  }

  public static Object delegate(MethodParameter parameter, Object attribute, Paging resolvedPaging) {
    Class<?> parameterType = parameter.getParameterType();
    ElementMatcher.Junction<MethodDescription> pageableMethodNames = ElementMatchers.namedOneOf("unpaged", "pageNumber", "pageSize", "orders", "convertTo");

    MethodDelegation pageableValuesInterceptor = MethodDelegation.to(new PageableValuesInterceptor(resolvedPaging));
    MethodDelegation pageableBeanAttributesInterceptor = MethodDelegation.to(new PageableBeanAttributesInterceptor(attribute));

    DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> buddyBuilder = new ByteBuddy()
      .subclass(parameterType)
      .method(pageableMethodNames)
      .intercept(pageableValuesInterceptor)
      .method(ElementMatchers.not(pageableMethodNames))
      .intercept(pageableBeanAttributesInterceptor);

    try (DynamicType.Unloaded<?> made = buddyBuilder.make()) {
      return made.load(parameterType.getClassLoader())
        .getLoaded()
        .getDeclaredConstructor()
        .newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public record PageableValuesInterceptor(Paging paging) {

    @RuntimeType
    public Object intercept(@Origin Method method) {
      return switch (method.getName()) {
        case "isUnpaged" -> paging.unpaged();
        case "pageNumber" -> paging.pageNumber();
        case "pageSize" -> paging.pageSize();
        case "orders" -> paging.orders();
        case "convertTo" -> PageConverterHolder.getConverter().to(paging);
        default -> throw new IllegalStateException();
      };
    }
  }

  public record PageableBeanAttributesInterceptor(Object target) {

    @RuntimeType
    public Object intercept(@This Object proxy, @Origin Method method, @AllArguments Object[] args) {
      try {
        return method.invoke(target, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
