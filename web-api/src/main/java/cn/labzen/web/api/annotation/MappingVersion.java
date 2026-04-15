package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * API 版本映射注解。
 * <p>
 * 注解在 Controller 接口或方法上，指定 API 的版本号。
 * 仅在 labzen.yml 中配置 {@code web.controller.version.enable = true} 时生效。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * public interface ResourceController extends StandardController&#60;ResourceService, ResourceBean, Long> {
 *
 *   &#64;Override
 *   &#64;MappingVersion(8)
 *   Result remove(Long id);
 * }
 * </pre>
 * <p>
 * 根据配置 {@code web.api-version.carrier} 的值，生成的 Controller 实现类中方法注解有所不同：
 * <p>
 * <b>URI 方式 (APIVersionCarrier.URI)</b>：
 * <pre>
 * public class ResourceControllerImpl {
 *   /**
 *    * 所有 API 请求路径会增加版本前缀
 *    * 如： DELETE /resource/{id} → DELETE /v1/resource/{id}
 *    * 默认版本取值为 web.controller.version.base，默认为 1
 *    * 使用 &#64;MappingVersion(8) 后，请求路径变为 DELETE /v8/resource/{id}
 *    &#42;/
 *   &#64;Override
 *   &#64;APIVersion("v8")
 *   &#64;DeleteMapping("{id}")
 *   public Result remove(&#64;PathVariable Long id) { ... }
 * }
 * </pre>
 * <p>
 * <b>HEADER 方式 (APIVersionCarrier.HEADER)</b>：
 * <pre>
 * public class ResourceControllerImpl {
 *   /**
 *    * 请求头中需包含版本信息，如 Accept: application/vnd.app.v1+json
 *    * vnd 名称通过 web.controller.version.header-vnd 配置，默认为 app
 *    * 使用 &#64;MappingVersion(8) 后，头信息变为 Accept: application/vnd.app.v8+json
 *    &#42;/
 *   &#64;Override
 *   &#64;DeleteMapping(value = "{id}", produces = {"application/vnd.app.v8+json"})
 *   public Result remove(&#64;PathVariable Long id) { ... }
 * }
 * </pre>
 * <p>
 * <b>PARAM 方式 (APIVersionCarrier.PARAM)</b>：
 * <pre>
 * public class ResourceControllerImpl {
 *   /**
 *    * 请求需传递版本参数，如 version=v1
 *    * 参数名通过 web.controller.version.param 设置
 *    * 使用 &#64;MappingVersion(8) 后，参数变为 version=v8
 *    &#42;/
 *   &#64;Override
 *   &#64;DeleteMapping(value = "{id}", params = {"version=v8"})
 *   public Result remove(&#64;PathVariable Long id) { ... }
 * }
 * </pre>
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
