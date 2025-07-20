package cn.labzen.web.service

import cn.labzen.web.controller.StandardController
import cn.labzen.web.response.bean.Result
import cn.labzen.web.response.bean.Results

/**
 * 兼容 [StandardController] 接口调用的业务逻辑层方法
 *
 * 该接口可快速辅助实现增删改查基础方法；如项目迭代久了，形成多个版本时，可参考该接口对增删改查的定义，
 * 修改不同版本的方法名，推荐方法名后加 `Vn` 其中 `n`为版本号
 *
 * @property R 资源类型
 * @property RI 资源Bean的主键ID类型
 */
@JvmDefaultWithCompatibility
interface StandardResourceService<R, RI> {

  /**
   * 创建资源记录
   */
  fun create(resource: R): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 修改资源记录
   */
  fun edit(id: RI, resource: R): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 删除资源记录
   */
  fun remove(id: RI): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 删除资源记录
   */
  fun removes(ids: Array<RI>): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 获取指定id的资源记录
   */
  fun info(id: RI): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 根据条件搜索资源记录
   */
  fun find(resource: R): Result {
    // will be implemented
    return Results.justSuccess()
  }

  /**
   * 根据条件导出资源记录为文件
   */
//  @Deprecated("改到 file-io 模块中实现", ReplaceWith("Results.justSuccess()", "cn.labzen.web.response.result.Result"))
//  fun export(
//    resource: R,
//    pagination: Paging,
//    exportCondition: ExportCondition
//  ): Result {
//    // will be implemented
//    return Results.justSuccess()
//  }
}