package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 指定业务逻辑层组件调用的注解。
 * <p>
 * 注解在 Controller 接口方法上，用于指定该方法需要调用的业务逻辑层组件（Service）及方法名。
 * <p>
 * 在不使用本注解的情况下，一个 Controller 接口方法，将会调用默认的业务逻辑层组件（XXXService）的同方法名：
 * <p>
 * <b>未使用 @Call 注解：</b>
 * <pre>
 * public interface ResourceController extends StandardController&#60;ResourceService, ResourceBean, Long> {
 *   &#47;**
 *    * 这个方法在实现类中，方法体代码为：`return resourceService.recache(id);`
 *    &#42;/
 *   &#64;PostMapping("/recache/{id}")
 *   Result recache(Long id);
 * }
 * </pre>
 * <p>
 * <b>使用 @Call 注解：</b>
 * <pre>
 * public interface ResourceController extends StandardController&#60;ResourceService, ResourceBean, Long> {
 *   &#47;**
 *    * 这个方法在实现类中，方法体代码为：
 *    * `return cachingHandlerService.recacheResource(id);`
 *    *
 *    * 同时，CachingHandlerService这个类实例也会注入到 Controller 接口实现类对象中
 *    &#42;/
 *   &#64;Call(target = CachingHandlerService.class, method = "recacheResource")
 *   &#64;PostMapping("/recache/{id}")
 *   Result recache(Long id);
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Call {

  /**
   * 调用的方法名，如为空，则默认调用目标方法名与 Controller 接口方法相同
   */
  String method() default "";

  /**
   * 调用的业务逻辑层组件类，将会在 Controller 实现类中注入
   */
  Class<?> target() default void.class;
}
