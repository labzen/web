package cn.labzen.web.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口或方法上，指定API的版本（在 labzen.yml 中配置 web.controller.version.enable = true 时有效）
 * <p>
 * ***示例：***
 * <code>
 * <pre>
 * public interface ResourceController extends StandardController&#60;ResourceService, ResourceBean, Long> {
 *
 *   &#64;Override
 *   &#64;MappingVersion(8)
 *   Result remove(Long id);
 * }
 * </pre>
 * </code>
 * <p>
 * 根据配置 web.api-version.carrier 的值，生成的 Controller 接口实现类中，方法上的注解有所不同：
 * <p>
 * <b>当取值为 APIVersionCarrier.URI</b>
 * <code>
 * <pre>
 * &#47;**
 *  * 生成的 Controller 实现类代码
 *  &#42;/
 * public class ResourceControllerImpl {
 *   &#47;**
 *    * 将会在方法上新声明一个注解 @APIVersion。
 *    *
 *    * 配置的项目中，所有的API请求路径都会增加一个前置版本标识，
 *    * 所有的方法默认是没有 @APIVersion 注解的，默认版本取值 web.controller.version.base，默认为 1
 *    * 例如：@DeleteMapping("resource/{id}") 对应的实际请求路径为：`DELETE /v1/resource/{id}`
 *    *
 *    * 因为有了版本指定，请求路径变为 `DELETE /v8/resource/{id}`
 *    &#42;/
 *   &#64;Override
 *   &#64;APIVersion("v8")
 *   &#64;DeleteMapping("{id}")
 *   public Result remove(@PathVariable Long id) {
 *     ...
 *   }
 * }
 * </pre>
 * </code>
 * <p>
 * <b>当取值为 APIVersionCarrier.HEADER</b>
 * <code>
 * <pre>
 * &#47;**
 *  * 生成的 Controller 实现类代码
 *  &#42;/
 * public class ResourceControllerImpl {
 *   &#47;**
 *    * 将会修改原先声明的 @RequestMapping 注解（或 @GetMapping、@PostMapping 等..）
 *    *
 *    * 配置的项目中，所有的API请求头信息中都需要定义一个版本信息，API的请求路径不受影响
 *    * 所有的API方法默认的版本取值 web.controller.version.base，默认为 1
 *    * 在头信息中需保证有 `Accept: application/vnd.app.v1+json`
 *    * vnd的名称通过 web.controller.version.header-vnd 配置，默认为 app
 *    *
 *    * 因为有了版本指定，头信息变为 `Accept: application/vnd.app.v8+json`
 *    &#42;/
 *   &#64;Override
 *   &#64;DeleteMapping(value = "{id}", produces = {"application/vnd.app.v8+json"})
 *   public Result remove(&#64;PathVariable Long id) {
 *     ...
 *   }
 * }
 * </pre>
 * </code>
 * <p>
 * <b>当取值为 APIVersionCarrier.PARAM</b>
 * <code>
 * <pre>
 * &#47;**
 *  * 生成的 Controller 实现类代码
 *  &#42;/
 * public class ResourceControllerImpl {
 *   &#47;**
 *    * 将会修改原先声明的 @RequestMapping 注解（或 @GetMapping、@PostMapping 等..）
 *    *
 *    * 配置的项目中，所有的API请求，都需要传递一个标识版本的 query param 参数，
 *    * 所有的API方法默认的版本取值 web.controller.version.base，默认为 1
 *    * 请求参数默认为 `version=v1`，参数名通过 web.controller.version.param 设置
 *    *
 *    * 因为有了版本指定，请求参数变为 `version=v8`
 *    &#42;/
 *   &#64;Override
 *   &#64;DeleteMapping(value = "{id}", params = {"version=v8"})
 *   public Result remove(@PathVariable Long id) {
 *     ...
 *   }
 * }
 * </pre>
 * </code>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MappingVersion {

  /**
   * 版本号
   */
  int value();
}
