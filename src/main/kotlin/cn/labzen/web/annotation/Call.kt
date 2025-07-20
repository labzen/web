package cn.labzen.web.annotation

import kotlin.reflect.KClass

/**
 * 注解在 Controller 接口方法上，指定在方法中调用的业务逻辑层组件（XXXService）及方法名
 *
 * 在不使用本注解的情况下，一个 Controller 接口方法，将会调用默认的业务逻辑层组件（XXXService）的同方法名
 * ```java
 * public interface ResourceController extends StandardController<ResourceService, ResourceBean, Long> {
 *
 *   /**
 *    * 这个方法在实现类中，方法体代码为：`return resourceService.recache(id);`
 *    */
 *   @PostMapping("/recache/{id}")
 *   Result recache(Long id);
 * }
 * ```
 *
 * 如果方法上声明本注解后：
 * ```java
 * public interface ResourceController extends StandardController<ResourceService, ResourceBean, Long> {
 *
 *   /**
 *    * 这个方法在实现类中，方法体代码为：`return cachingHandlerService.recacheResource(id);`
 *    *
 *    * 同时，CachingHandlerService这个类实例也会注入到 Controller 接口实现类对象中
 *    */
 *   @Call(target = CachingHandlerService.class, method = "recacheResource")
 *   @PostMapping("/recache/{id}")
 *   Result recache(Long id);
 * }
 * ```
 *
 * - [method] 调用的方法名，如为空，则默认调用目标方法名与 Controller 接口方法相同
 * - [target] 调用的业务逻辑层组件类，将会在 Controller 实现类中注入
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Call(
  val method: String = "",
  val target: KClass<*> = Any::class
)
