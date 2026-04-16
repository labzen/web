package cn.labzen.web.paging.internal;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebCoreConfiguration;
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

/**
 * Pageable 动态代理工厂
 * <p>
 * 使用 ByteBuddy 为实现了 {@link Pageable} 接口的类创建动态代理，
 * 使得方法调用能够访问解析后的分页数据。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>拦截 Pageable 接口定义的方法调用，返回解析后的分页数据</li>
 *   <li>代理其他方法调用到原始 Bean 实例</li>
 *   <li>支持调试模式：保留原始字段用于调试观察</li>
 * </ul>
 */
public final class PageableDelegator {

  private static final String DEBUGGER_PAGING_FIELD_NAME = "_paging";
  private static final Objenesis OBJENESIS = new ObjenesisStd();
  // Pageable接口定义的几个方法
  private static final ElementMatcher.Junction<MethodDescription> PAGEABLE_METHOD_NAMES = ElementMatchers.namedOneOf("unpaged", "pageNumber", "pageSize", "orders", "convertTo");

  private static final boolean FRIENDLY_FOR_DEBUGGER_VIEW;

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    FRIENDLY_FOR_DEBUGGER_VIEW = configuration.debug();
  }

  private PageableDelegator() {
  }

  /**
   * 创建 Pageable 代理对象
   *
   * @param parameter 方法参数信息
   * @param attribute 已绑定的 Bean 实例
   * @param resolvedPaging 解析后的分页条件
   * @return 代理后的对象
   */
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
   * 直接代理模式
   * <p>
   * 使用 ByteBuddy 创建子类代理，拦截 Pageable 接口方法，
   * 其他方法委托给原始 Bean 实例。
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
   * 调试模式代理
   * <p>
   * 在代理对象中额外添加私有字段保存分页数据和原始 Bean，
   * 便于在调试器中观察属性值。
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

  /**
   * 复制源对象的字段到目标对象
   * <p>
   * 递归遍历父类层次，复制所有声明的字段值。
   */
  private static void copyFields(Object source, Object target) {
    Class<?> clazz = source.getClass();
    while (clazz != null && clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        try {
          f.setAccessible(true);
          f.set(target, f.get(source));
        } catch (IllegalAccessException ignored) {
          // ignored
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  /**
   * 注入分页数据到代理对象的私有字段
   */
  private static void injectPaging(Class<?> proxyType, Object proxy, Paging paging) {
    try {
      Field p = proxyType.getDeclaredField(DEBUGGER_PAGING_FIELD_NAME);
      p.setAccessible(true);
      p.set(proxy, paging);
    } catch (Exception ignored) {
      // ignored
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
