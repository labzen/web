package cn.labzen.web.annotation

import cn.labzen.web.service.BaseResourceService
import kotlin.reflect.KClass

/**
 * 基于资源提供增删改查等基本接口（符合RESTFUL）
 *
 * 本注解依赖于[ServiceHandler]，各基本接口的实现，将调用[ServiceHandler.value]所指定的Service中对应的方法
 *
 * Controller API 将调用 Service 中使用 methodXxx 指定的方法，例如使用 [methodCreate] 定义的方法（默认为 'create'）；如果不想生成某个方法，则将对应的MethodXxx的值置为空字符。
 *
 * - [resource] 指定Restful接口处理的资源Bean，可以是ORM的domain bean或是DTO（建议）
 * - [resourceId] 指定ResourceBean的ID属性名，默认 id
 * - [resourceIdType] 指定ResourceBean的ID类型，默认是Long
 * - [methodCreate] 指定新增资源的Service方法名，方法参数1个 (resourceBean)，参考[BaseResourceService]
 * - [methodRemove] 指定删除资源的Service方法名，方法参数1个 (resourceId)，参考[BaseResourceService]
 * - [methodEdit] 指定修改资源的Service方法名，方法参数1个 (resourceBean)，参考[BaseResourceService]
 * - [methodInfo] 指定返回指定ID资源信息的Service方法名，方法参数1个 (resourceId)，参考[BaseResourceService]
 * - [methodAll] 指定返回所有资源信息的Service方法名，方法参数0个 ()，参考[BaseResourceService]
 * - [methodFind] 指定通过资源某（些）个属性搜索的资源结果集合的Service方法名，方法参数2个 (resourceBean, paginationCondition)，参考[BaseResourceService]
 * - [methodExport] 指定导出资源（可指定条件）Excel的Service方法名，方法参数1个 (resourceBean)，默认空字符串，不实现
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@LabzenWeb
annotation class BaseResource(
  // todo 权限相关的注解
  val resource: KClass<*>,
  val resourceId: String = "id",
  val resourceIdType: KClass<*> = Long::class,
  val methodCreate: Array<String> = ["create"],
  val methodRemove: Array<String> = ["remove"],
  val methodEdit: Array<String> = ["edit"],
  val methodInfo: Array<String> = ["info"],
  val methodAll: Array<String> = ["all"],
  val methodFind: Array<String> = ["find"],
  val methodExport: Array<String> = ["export"]
)
