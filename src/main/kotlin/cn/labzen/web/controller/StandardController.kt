package cn.labzen.web.controller

import cn.labzen.web.annotation.*
import cn.labzen.web.response.bean.Result
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 基于Restful的标准Spring Controller标准定义模板。继承本接口（是继承！）的接口，会快速拥有一个标准的Controller实现。
 *
 * 继承本接口就无需再继承 [SimplestController] 了
 *
 * ***A.*** 本接口的三个泛型定义：
 * ```
 * - BS: Business Service Component - 指定当前Controller入口需要调用的业务逻辑处理类，一般为XXXService
 * - RB: Resource Bean - 指定当前Controller处理的资源Bean，所有的资源传递都将通过该Bean作为参数的包装容器
 * - ID: Resource Bean ID Type - 资源Bean的主键类型
 * ```
 *
 * ***B.*** 继承[StandardController]的Controller，如果在业务上不需要某一个入口，例如：DELETE /resource/{id}，则将对应的方法注解一个 @[Abandoned] 即可，参考 `示例 1`。
 *
 * ***C.*** [StandardController]无法满足业务需求的场景下，开发者可像常规的 `Spring Controller Class` 定义一个入口方法。
 * 为了保证Controller的 `单一职责原则`，在使用 `Labzen Web` 组件定义的 Controller接口中，不允许出现 `default Result customMethod(...) { ... }` 这种自定义方法，进行复杂的代码处理。
 * 参考 `示例 2` 快速定义一个入口，该方法将调用 [BS] 业务逻辑层进行处理，请确保业务逻辑层的方法存在。***注意：请统一返回类型为Result***
 *
 * ***X.*** 在Controller定义接口中，还有一些实现其他功能的注解，可参考：
 * - [Abandoned]：弃用API入口
 * - [Caching]：对API响应内容进行缓存
 * - [Call]：在方法上注解，标识将调用指定的其他 Component 的指定方法，如果不是特殊业务需要，应保持Controller和业务处理访问的关系一致性，所以不建议使用
 * - [Catching]：`Labzen Web` 组件有统一的异常信息处理，如需对某个入口产生异常时，进行特定的处理，可使用该注解
 * - [Crypto]：对API入参或响应内容进行加密配置
 * - [MappingVersion]：定义API入口的版本号
 * - [Monitor]：对API接口进行监控，配置监控项
 * - [Threshold]：调整入口的并发访问域值等相关参数及熔断方式
 *
 * > 在Controller定义接口中的方法上，声明的所有注解都将会在生成的Controller实现类中被擦除
 *
 * - **示例 1**
 * ```java
 * /**
 *  * 这是一个标准的Controller定义，包含了增改查等常用入口定义。这里使用 @Abandoned 注解剔除了删除资源的入口
 *  */
 * public interface ResourceController extends StandardController<ResourceService, ResourceBean, Long> {
 *
 *   @Abandoned
 *   Result remove(Long id);
 * }
 * ```
 * - **示例 2**
 * ```java
 * /**
 *  * 这是一个标准的Controller定义
 *  */
 * public interface ResourceController extends StandardController<ResourceService, ResourceBean, Long> {
 *
 *   /**
 *    * 这里定义了一个快速入口
 *    */
 *   @PostMapping("/recache/{id}")
 *   Result recache(@PathVariable Long id);
 * }
 * ```
 *
 * @param <BS> Business Service Component
 * @param <RB> Resource Bean
 * @param <ID> Resource Bean ID Type
 */
interface StandardController<BS, RB, ID> : LabzenController {

  /**
   * 示例：Restful API 创建资源 - POST /resources
   */
  @PostMapping
  fun create(@Validated @ModelAttribute resource: RB): Result

  /**
   * 示例：Restful API 修改资源 - POST /resources/{id}
   */
  @PutMapping("{id}")
  fun edit(@PathVariable id: ID, @Validated @ModelAttribute resource: RB): Result

  /**
   * 示例：Restful API 删除资源 - DELETE /resources/{id}
   */
  @DeleteMapping("{id}")
  fun remove(@PathVariable id: ID): Result

  /**
   * 示例：Restful API 批量删除资源 - DELETE /resources/batch?ids=1,2,3
   */
  @DeleteMapping("batch")
  fun removes(@RequestParam ids: Array<ID>): Result

  /**
   * 示例：Restful API 获取单个资源详情 - GET /resources/{id}
   */
  @GetMapping("{id}")
  fun info(@PathVariable id: ID): Result

  /**
   * 示例：Restful API 分页/条件查询列表 - GET /resources
   */
  @GetMapping
  fun find(@ModelAttribute resource: RB): Result
}