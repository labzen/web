package cn.labzen.web.service

import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.request.PagingCondition
import cn.labzen.web.response.result.Result
import javax.annotation.Nonnull

/**
 * 用于快速创建支持注解了[BaseResource]的Controller调用的Service
 *
 * @property R 资源类型
 * @property RI 资源Bean的主键ID类型
 */
@JvmDefaultWithCompatibility
interface BaseResourceService<R, RI> {

  /**
   * 创建资源记录，对应注解[BaseResource.create]默认的方法，如果开发者修改了[BaseResource.create]的方法名，请忽略该方法实现
   */
  fun create(@Nonnull resource: R): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 删除资源记录，对应注解[BaseResource.remove]默认的方法，如果开发者修改了[BaseResource.remove]的方法名，请忽略该方法实现
   */
  fun remove(@Nonnull id: RI): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 修改资源记录，对应注解[BaseResource.edit]默认的方法，如果开发者修改了[BaseResource.edit]的方法名，请忽略该方法实现
   */
  fun edit(@Nonnull resource: R): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 获取指定id的资源记录，对应注解[BaseResource.info]默认的方法，如果开发者修改了[BaseResource.info]的方法名，请忽略该方法实现
   */
  fun info(@Nonnull id: RI): Result {
    // will be implemented
    return Result.justSuccess()
  }

  /**
   * 返回所有的资源记录，对应注解[BaseResource.all]默认的方法，如果开发者修改了[BaseResource.all]的方法名，请忽略该方法实现
   */
  fun all(): Result {
    return Result.justSuccess()
  }

  /**
   * 根据条件搜索资源记录，对应注解[BaseResource.find]默认的方法，如果开发者修改了[BaseResource.find]的方法名，请忽略该方法实现
   */
  fun find(@Nonnull resource: R?, @Nonnull pagination: PagingCondition?): Result {
    // will be implemented
    return Result.justSuccess()
  }
}