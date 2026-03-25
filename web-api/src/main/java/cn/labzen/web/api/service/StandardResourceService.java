package cn.labzen.web.api.service;

import cn.labzen.web.api.controller.StandardController;
import cn.labzen.web.api.response.result.Result;
import cn.labzen.web.api.response.result.Results;

import java.util.List;

/**
 * 兼容 {@link StandardController} 接口调用的业务逻辑层方法
 * <p>
 * 该接口可快速辅助实现增删改查基础方法；如项目迭代久了，形成多个版本时，可参考该接口对增删改查的定义，
 * 修改不同版本的方法名，推荐方法名后加 `Vn` 其中 `n`为版本号
 *
 * @param <RB> 资源类型（Resource Bean）
 * @param <ID> 资源 Bean 的主键 ID 类型
 */
public interface StandardResourceService<RB, ID> {

  /**
   * 创建资源记录
   *
   * @param resource 待创建的资源对象
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result create(RB resource) {
    // will be implemented
    return Results.success();
  }

  /**
   * 修改指定 ID 的资源记录
   *
   * @param id       资源主键 ID
   * @param resource 待更新的资源对象
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result edit(ID id, RB resource) {
    // will be implemented
    return Results.success();
  }

  /**
   * 删除指定 ID 的资源记录
   *
   * @param id 资源主键 ID
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result remove(ID id) {
    // will be implemented
    return Results.success();
  }

  /**
   * 批量删除资源记录
   *
   * @param ids 资源主键 ID 列表
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result removes(List<ID> ids) {
    // will be implemented
    return Results.success();
  }

  /**
   * 获取指定 ID 的资源记录详情
   *
   * @param id 资源主键 ID
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result info(ID id) {
    // will be implemented
    return Results.success();
  }

  /**
   * 根据条件搜索资源记录
   *
   * @param resource 查询条件封装的资源对象
   * @return 使用{@link Results}快速创建结果封装对象
   */
  default Result find(RB resource) {
    // will be implemented
    return Results.success();
  }
}
