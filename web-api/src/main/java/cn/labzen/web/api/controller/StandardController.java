package cn.labzen.web.api.controller;

import cn.labzen.web.api.annotation.*;
import cn.labzen.web.api.response.result.Result;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标准 Restful Controller 接口模板。
 * <p>
 * 继承本接口（注意：是继承而非实现）后，可快速拥有一个标准的 Controller 实现。
 * 继承本接口后无需再继承 {@link SimplestController}。
 * <p>
 * 如果业务上不需要某个接口方法（如 DELETE /resource/{id}），
 * 可使用 {@link Abandoned} 注解标记该方法将其排除。参考<b>示例 1</b>。
 * <p>
 * 当本接口无法满足业务需求时，开发者可像定义常规 Spring Controller Class 一样定义入口方法。
 * 为保证 Controller 的<b>单一职责原则</b>，不建议在接口中定义 {@code default Result customMethod(...)} 自定义方法。
 * 参考<b>示例 2</b> 快速定义入口，该方法将调用 {@link BS} 业务逻辑层进行处理，返回类型必须为 {@link Result}。
 * <p>
 * <b>可用的扩展注解：</b>
 * <ul>
 *   <li>{@link Abandoned}：弃用 API 入口</li>
 *   <li>{@link Caching}：对 API 响应内容进行缓存</li>
 *   <li>{@link Call}：指定调用其他 Component 的方法（不建议使用）</li>
 *   <li>{@link Catching}：对特定异常进行定制处理</li>
 *   <li>{@link Crypto}：对 API 响应内容进行加密</li>
 *   <li>{@link MappingVersion}：定义 API 入口的版本号</li>
 *   <li>{@link Monitor}：对 API 接口进行监控</li>
 *   <li>{@link Threshold}：调整接口的并发访问阈值及熔断策略</li>
 * </ul>
 * <p>
 * <b>注意：</b> 在 Controller 定义接口中的方法上声明的所有注解，都将在生成的 Controller 实现类中被擦除。
 * <hr/>
 * <b>示例 1：</b> 使用 @Abandoned 剔除删除入口
 * <pre>
 * /**
 *  * 这是一个标准的Controller定义，包含了增改查等常用入口定义。
 *  * 这里使用 @Abandoned 注解剔除了删除资源的入口
 *  &#42;/
 * public interface ResourceController extends StandardController<resourceervice, ResourceBean, Long> {
 *   &#64;Abandoned
 *   Result remove(Long id);  // 删除接口将被排除
 * }
 * </pre>
 * <hr/>
 * <b>示例 2：</b> 定义快速入口
 * <pre>
 * /**
 *  * 这是一个标准的Controller定义
 *  &#42;/
 * public interface ResourceController extends StandardController<resourceervice, ResourceBean, Long> {
 *   /**
 *    * 这里定义了一个快速入口
 *    &#42;/
 *   &#64;PostMapping("/recache/{id}")
 *   Result recache(&#64;PathVariable Long id);
 * }
 * </pre>
 *
 * @param <BS> 业务服务组件类，指定当前 Controller 入口需要调用的业务逻辑处理类（通常为 XXXService）
 * @param <RB> 资源 Bean，指定当前 Controller 处理的资源对象，所有资源传递都通过该 Bean 作为容器
 * @param <ID> 资源主键类型
 */
public interface StandardController<BS, RB, ID> extends LabzenController {

  /**
   * 示例：Restful API 创建资源 - POST /resource
   */
  @PostMapping
  Result create(@Validated @ModelAttribute RB resource);

  /**
   * 示例：Restful API 修改资源 - PUT /resource/{id}
   */
  @PutMapping("{id:\\d{1,19}}")
  Result edit(@PathVariable ID id, @Validated @ModelAttribute RB resource);

  /**
   * 示例：Restful API 删除资源 - DELETE /resource/{id}
   */
  @DeleteMapping("{id:\\d{1,19}}")
  Result remove(@PathVariable ID id);

  /**
   * 示例：Restful API 批量删除资源 - DELETE /resource?ids=1,2,3
   */
  @DeleteMapping
  Result removes(@NotEmpty @RequestParam("ids") List<@NotNull ID> ids);

  /**
   * 示例：Restful API 获取单个资源详情 - GET /resource/{id}
   */
  @GetMapping("{id:\\d{1,19}}")
  Result info(@PathVariable ID id);

  /**
   * 示例：Restful API 分页/条件查询列表 - GET /resource
   */
  @GetMapping
  Result find(@ModelAttribute RB resource);
}
