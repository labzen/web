package cn.labzen.web.paging.internal;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.meta.Labzens;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.paging.convert.PageConverterHolder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static cn.labzen.web.api.definition.Constants.LOGGER_SCENE_PAGING;

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
 *   <li>生成的代理类按资源类型缓存；分页数据与原始实例存放在<b>代理实例字段</b>上，
 *       每次调用独立注入，缓存不共享请求状态</li>
 * </ul>
 */
public final class PageableDelegator {

  private static final String PAGING_FIELD_NAME = "_paging";
  private static final String TARGET_FIELD_NAME = "_target";
  // Pageable接口定义的几个方法
  private static final ElementMatcher.Junction<MethodDescription> PAGEABLE_METHOD_NAMES = ElementMatchers.namedOneOf(
      "unpaged",
      "pageNumber",
      "pageSize",
      "orders",
      "convertTo");
  // 缓存已生成的代理类，避免每次请求都重新生成导致 Metaspace OOM
  private static final ConcurrentHashMap<Class<?>, Class<?>> PROXY_CLASS_CACHE = new ConcurrentHashMap<>();
  private static final boolean FRIENDLY_FOR_DEBUGGER_VIEW;
  private static final LabzenLogger LOGGER = Loggers.getLogger(PageableDelegator.class);

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    FRIENDLY_FOR_DEBUGGER_VIEW = configuration.debug();
  }

  private PageableDelegator() {
  }

  /**
   * 创建 Pageable 代理对象
   *
   * @param parameter      方法参数信息
   * @param attribute      已绑定的 Bean 实例
   * @param resolvedPaging 解析后的分页条件
   * @return 代理后的对象
   */
  public static Object delegate(MethodParameter parameter, Object attribute, Paging resolvedPaging) {
    Class<?> parameterType = parameter.getParameterType();
    Class<?> proxyType = PROXY_CLASS_CACHE.computeIfAbsent(parameterType, PageableDelegator::defineProxyType);

    Object proxy = instantiate(proxyType);
    inject(proxyType, proxy, PAGING_FIELD_NAME, resolvedPaging);
    inject(proxyType, proxy, TARGET_FIELD_NAME, attribute);

    if (FRIENDLY_FOR_DEBUGGER_VIEW) {
      copyFields(attribute, proxy);
    }
    return proxy;
  }

  /**
   * 定义代理类型
   * <p>
   * 生成子类代理，拦截 Pageable 接口方法读取实例上的分页数据，其他方法委托给实例上的原始 Bean。
   */
  private static Class<?> defineProxyType(Class<?> parameterType) {
    DynamicType.Builder<?> buddyBuilder = new ByteBuddy().subclass(parameterType)
                                                         .defineField(PAGING_FIELD_NAME,
                                                             Paging.class,
                                                             Visibility.PRIVATE,
                                                             SyntheticState.SYNTHETIC)
                                                         .defineField(TARGET_FIELD_NAME,
                                                             Object.class,
                                                             Visibility.PRIVATE,
                                                             SyntheticState.SYNTHETIC)
                                                         .method(PAGEABLE_METHOD_NAMES)
                                                         .intercept(MethodDelegation.to(PageableValuesInterceptor.class))
                                                         .method(ElementMatchers.not(PAGEABLE_METHOD_NAMES))
                                                         .intercept(MethodDelegation.to(
                                                             PageableBeanAttributesInterceptor.class));

    try (DynamicType.Unloaded<?> made = buddyBuilder.make()) {
      return made.load(parameterType.getClassLoader()).getLoaded();
    }
  }

  private static Object instantiate(Class<?> proxyType) {
    try {
      return proxyType.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("无法创建分页代理实例：" + proxyType.getName(), e);
    }
  }

  private static void inject(Class<?> proxyType, Object proxy, String fieldName, Object value) {
    try {
      Field field = proxyType.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(proxy, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("无法注入分页代理字段：" + fieldName, e);
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
        } catch (IllegalAccessException e) {
          LOGGER.atWarn()
                .scene(LOGGER_SCENE_PAGING)
                .status(Status.FAILED)
                .setCause(e)
                .log("无法复制字段 {} 的值", f.getName());
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  /**
   * 处理 {@link Pageable} 接口定义的几个接口参数获取
   */
  @SuppressWarnings("unused")
  public static final class PageableValuesInterceptor {

    @RuntimeType
    public static Object intercept(@FieldValue(PAGING_FIELD_NAME) Paging paging, @Origin Method method) {
      return switch (method.getName()) {
        case "unpaged" -> paging.unpaged();
        case "pageNumber" -> paging.pageNumber();
        case "pageSize" -> paging.pageSize();
        case "orders" -> paging.orders();
        case "convertTo" -> PageConverterHolder.getConverter().to(paging);
        default -> throw new IllegalStateException("未处理的分页方法：" + method.getName());
      };
    }
  }

  /**
   * 处理实现了 {@link Pageable} 接口的 Bean Class 的其他参数获取（除接口定义的方法）
   */
  @SuppressWarnings("unused")
  public static final class PageableBeanAttributesInterceptor {

    @RuntimeType
    public static Object intercept(@FieldValue(TARGET_FIELD_NAME) Object target,
                                   @Origin Method method,
                                   @AllArguments Object[] args) {
      try {
        return method.invoke(target, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
