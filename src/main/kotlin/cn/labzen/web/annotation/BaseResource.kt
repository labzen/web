package cn.labzen.web.annotation

import kotlin.reflect.KClass
import cn.labzen.web.service.BaseResourceService

/**
 * 基于资源提供增删改查等基本接口（符合RESTFUL）
 *
 * 本注解依赖于[ServiceHandler]，各基本接口的实现，将调用[ServiceHandler.main]所指定的Service中对应的方法
 *
 * 如将某个Service方法名指定为空字符串，该基本接口将不生效
 *
 * @param resource 指定Restful接口处理的资源Bean，可以是ORM的domain bean或是DTO（建议）
 * @param resourceId 指定ResourceBean的ID属性名，默认 id
 * @param resourceIdType 指定ResourceBean的ID类型，默认是Long
 * @param create 指定新增资源的Service方法名，方法参数1个 (resourceBean)，参考[BaseResourceService]
 * @param remove 指定删除资源的Service方法名，方法参数1个 (resourceId)，参考[BaseResourceService]
 * @param edit 指定修改资源的Service方法名，方法参数1个 (resourceBean)，参考[BaseResourceService]
 * @param info 指定返回指定ID资源信息的Service方法名，方法参数1个 (resourceId)，参考[BaseResourceService]
 * @param all 指定返回所有资源信息的Service方法名，方法参数0个 ()，参考[BaseResourceService]
 * @param find 指定通过资源某（些）个属性搜索的资源结果集合的Service方法名，方法参数2个 (resourceBean, paginationCondition)，参考[BaseResourceService]
 * @param export 指定导出资源（可指定条件）Excel的Service方法名，方法参数1个 (resourceBean)，默认空字符串，不实现
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
  val create: String = "create",
  val remove: String = "remove",
  val edit: String = "edit",
  val info: String = "info",
  val all: String = "all",
  val find: String = "find",
  val export: String = ""
)
