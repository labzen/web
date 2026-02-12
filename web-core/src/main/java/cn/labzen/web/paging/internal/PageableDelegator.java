package cn.labzen.web.paging.internal;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebConfiguration;
import cn.labzen.web.paging.convert.PageConverterHolder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PageableDelegator {

  private static final String DEBUGGER_PAGING_FIELD_NAME = "_paging";
  private static final Objenesis OBJENESIS = new ObjenesisStd();
  // Pageable接口定义的几个方法
  private static final ElementMatcher.Junction<MethodDescription> PAGEABLE_METHOD_NAMES = ElementMatchers.namedOneOf("unpaged", "pageNumber", "pageSize", "orders", "convertTo");

  private static final boolean FRIENDLY_FOR_DEBUGGER_VIEW;

  static {
    WebConfiguration configuration = Labzens.configurationWith(WebConfiguration.class);
    FRIENDLY_FOR_DEBUGGER_VIEW = configuration.debug();
  }

  private PageableDelegator() {
  }

  public static Object delegate(MethodParameter parameter, Object attribute, Paging resolvedPaging) {
    Class<?> parameterType = parameter.getParameterType();

    PageableValuesInterceptor pageableInterceptor = new PageableValuesInterceptor(resolvedPaging);
    PageableBeanAttributesInterceptor beanAttributesInterceptor = new PageableBeanAttributesInterceptor(attribute);

    if (FRIENDLY_FOR_DEBUGGER_VIEW) {
      return delegateForDebugger(parameterType, pageableInterceptor, beanAttributesInterceptor);
    } else {
      return delegateDirectly(parameterType, pageableInterceptor, beanAttributesInterceptor);
    }
  }

  /**
   * 直接代理，不需要进行特殊处理
   */
  private static Object delegateDirectly(Class<?> parameterType, PageableValuesInterceptor pageableInterceptor, PageableBeanAttributesInterceptor beanAttributesInterceptor) {
    DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> buddyBuilder = new ByteBuddy()
      .subclass(parameterType)
      .method(PAGEABLE_METHOD_NAMES)
      .intercept(MethodDelegation.to(pageableInterceptor))
      .method(ElementMatchers.not(PAGEABLE_METHOD_NAMES))
      .intercept(MethodDelegation.to(beanAttributesInterceptor));

    try (DynamicType.Unloaded<?> made = buddyBuilder.make()) {
      return made.load(parameterType.getClassLoader())
        .getLoaded()
        .getDeclaredConstructor()
        .newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 创建针对调试窗口友好的代理对象，用于在调试过程中，能够直接观察到被代理对象的属性值，适合在开发阶段使用
   */
  private static Object delegateForDebugger(Class<?> parameterType, PageableValuesInterceptor pageableInterceptor, PageableBeanAttributesInterceptor beanAttributesInterceptor) {
    DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> buddyBuilder = new ByteBuddy()
      .subclass(parameterType)
      .defineField(DEBUGGER_PAGING_FIELD_NAME, Paging.class, Visibility.PRIVATE)
      .method(PAGEABLE_METHOD_NAMES)
      .intercept(MethodDelegation.to(pageableInterceptor))
      .method(ElementMatchers.not(PAGEABLE_METHOD_NAMES))
      .intercept(MethodDelegation.to(beanAttributesInterceptor));

    try (DynamicType.Unloaded<?> unloaded = buddyBuilder.make()) {
      Class<?> proxyType = unloaded.load(parameterType.getClassLoader())
        .getLoaded();

      // 使用 Objenesis 创建实例，不调用构造函数
      Object proxy = OBJENESIS.newInstance(proxyType);
      // 把原 attribute 的字段复制到 proxy 上
      copyFields(beanAttributesInterceptor.getTarget(), proxy);
      // 把 Paging 注入（用于 toString）
      injectPaging(proxyType, proxy, pageableInterceptor.paging);

      return proxy;
    }
  }

  private static void copyFields(Object source, Object target) {
    Class<?> clazz = source.getClass();
    while (clazz != null && clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        try {
          f.setAccessible(true);
          f.set(target, f.get(source));
        } catch (IllegalAccessException ignored) {
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private static void injectPaging(Class<?> proxyType, Object proxy, Paging paging) {
    try {
      Field p = proxyType.getDeclaredField(DEBUGGER_PAGING_FIELD_NAME);
      p.setAccessible(true);
      p.set(proxy, paging);
    } catch (Exception ignored) {
    }
  }

  /**
   * 处理 {@link Pageable} 接口定义的几个接口参数获取
   */
  @RequiredArgsConstructor
  public static final class PageableValuesInterceptor {

    @Getter(AccessLevel.PACKAGE)
    private final Paging paging;

    @RuntimeType
    public Object intercept(@Origin Method method) {
      return switch (method.getName()) {
        case "unpaged" -> paging.unpaged();
        case "pageNumber" -> paging.pageNumber();
        case "pageSize" -> paging.pageSize();
        case "orders" -> paging.orders();
        case "convertTo" -> PageConverterHolder.getConverter().to(paging);
        default -> throw new IllegalStateException();
      };
    }
  }

  /**
   * 处理实现了 {@link Pageable} 接口的 Bean Class 的其他参数获取（除接口定义的方法）
   */
  @RequiredArgsConstructor
  public static final class PageableBeanAttributesInterceptor {

    @Getter(AccessLevel.PACKAGE)
    private final Object target;

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
