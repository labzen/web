package cn.labzen.web.service

import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.annotation.MappingVersion
import cn.labzen.web.request.ExportCondition
import cn.labzen.web.request.PagingCondition
import cn.labzen.web.response.result.Result

/**
 * 用于快速创建支持注解了[BaseResource]的Controller调用的Service
 *
 * 当禁用 API 版本控制，或默认版本 1 时，该接口可快速辅助实现增删改查 Service 方法；如项目迭代久了，形成多个版本时，可参考该接口对增删改查
 * 的定义，修改不同版本的方法名，并在 Controller 接口上注释的 [BaseResource] 中，将不同版本的方法名，设置到 methodXxx 参数中
 * （如：methodCreate），同时，需要在 Service 方法上注解 [MappingVersion] 来显示的指明版本号
 *
 * @property R 资源类型
 * @property RI 资源Bean的主键ID类型
 */
@JvmDefaultWithCompatibility
interface BaseResourceService<R, RI> {

  /**
   * 创建资源记录，对应注解[BaseResource.methodCreate]默认的方法，如果开发者修改了[BaseResource.methodCreate]的方法名，请忽略该方法实现
   */
  fun create(resource: R): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 删除资源记录，对应注解[BaseResource.methodRemove]默认的方法，如果开发者修改了[BaseResource.methodRemove]的方法名，请忽略该方法实现
   */
  fun remove(id: RI): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 修改资源记录，对应注解[BaseResource.methodEdit]默认的方法，如果开发者修改了[BaseResource.methodEdit]的方法名，请忽略该方法实现
   */
  fun edit(resource: R): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 获取指定id的资源记录，对应注解[BaseResource.methodInfo]默认的方法，如果开发者修改了[BaseResource.methodInfo]的方法名，请忽略该方法实现
   */
  fun info(id: RI): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 返回所有的资源记录，对应注解[BaseResource.methodAll]默认的方法，如果开发者修改了[BaseResource.methodAll]的方法名，请忽略该方法实现
   */
  fun all(): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 根据条件搜索资源记录，对应注解[BaseResource.methodFind]默认的方法，如果开发者修改了[BaseResource.methodFind]的方法名，请忽略该方法实现
   */
  fun find(resource: R, pageCondition: PagingCondition): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 根据条件导出资源记录为文件，对应注解[BaseResource.methodExport]默认的方法，如果开发者修改了[BaseResource.methodExport]的方法名，请忽略该方法实现
   */
  fun export(
    resource: R,
    pagination: PagingCondition,
    exportCondition: ExportCondition
  ): Result {
    // will be implemented
    return Result.justSuccess()
  }
}