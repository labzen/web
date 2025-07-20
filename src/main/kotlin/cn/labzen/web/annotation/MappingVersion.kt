package cn.labzen.web.annotation

/**
 * 注解在 Controller 接口或方法上，指定API的版本（在 labzen.yml 中配置 web.controller.version.enable = true 时有效）
 *
 * ***示例：***
 * ```java
 * public interface ResourceController extends StandardController<ResourceService, ResourceBean, Long> {
 *
 *   @Override
 *   @MappingVersion(8)
 *   Result remove(Long id);
 * }
 * ```
 * 根据配置 web.controller.version.place 的值，生成的 Controller 接口实现类中，方法上的注解有所不同：
 * - URI
 * ```java
 * /**
 *  * 生成的 Controller 实现类代码
 *  */
 * public class ResourceControllerImpl {
 *
 *   /**
 *    * 将会在方法上新声明一个注解 @APIVersion。
 *    *
 *    * 配置的项目中，所有的API请求路径都会增加一个前置版本标识，
 *    * 所有的方法默认是没有 @APIVersion 注解的，默认版本取值 web.controller.version.base，默认为 1
 *    * 例如：@DeleteMapping("resource/{id}") 对应的实际请求路径为：`DELETE /v1/resource/{id}`
 *    *
 *    * 因为有了版本指定，请求路径变为 `DELETE /v8/resource/{id}`
 *    */
 *   @Override
 *   @APIVersion("v8")
 *   @DeleteMapping("{id}")
 *   public Result remove(@PathVariable Long id) {
 *     ...
 *   }
 * }
 * ```
 *
 * - HEADER
 * ```java
 * /**
 *  * 生成的 Controller 实现类代码
 *  */
 * public class ResourceControllerImpl {
 *
 *   /**
 *    * 将会修改原先声明的 @RequestMapping 注解（或 @GetMapping、@PostMapping 等..）
 *    *
 *    * 配置的项目中，所有的API请求头信息中都需要定义一个版本信息，API的请求路径不受影响
 *    * 所有的API方法默认的版本取值 web.controller.version.base，默认为 1
 *    * 在头信息中需保证有 `Accept: application/vnd.app.v1+json`
 *    * vnd的名称通过 web.controller.version.header-vnd 配置，默认为 app
 *    *
 *    * 因为有了版本指定，头信息变为 `Accept: application/vnd.app.v8+json`
 *    */
 *   @Override
 *   @DeleteMapping(value = "{id}", produces = {"application/vnd.app.v8+json"})
 *   public Result remove(@PathVariable Long id) {
 *     ...
 *   }
 * }
 * ```
 *
 * - PARAM
 * ```java
 * /**
 *  * 生成的 Controller 实现类代码
 *  */
 * public class ResourceControllerImpl {
 *
 *   /**
 *    * 将会修改原先声明的 @RequestMapping 注解（或 @GetMapping、@PostMapping 等..）
 *    *
 *    * 配置的项目中，所有的API请求，都需要传递一个标识版本的 query param 参数，
 *    * 所有的API方法默认的版本取值 web.controller.version.base，默认为 1
 *    * 请求参数默认为 `version=v1`，参数名通过 web.controller.version.param 设置
 *    *
 *    * 因为有了版本指定，请求参数变为 `version=v8`
 *    */
 *   @Override
 *   @DeleteMapping(value = "{id}", params = {"version=v8"})
 *   public Result remove(@PathVariable Long id) {
 *     ...
 *   }
 * }
 * ```
 *
 * - [value] 版本号
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class MappingVersion(
  val value: Int
)
