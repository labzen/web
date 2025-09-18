package cn.labzen.web.service;

import cn.labzen.web.controller.StandardController;
import cn.labzen.web.response.Results;
import cn.labzen.web.response.bean.Result;

/**
 * 兼容 {@link StandardController} 接口调用的业务逻辑层方法
 * <p>
 * 该接口可快速辅助实现增删改查基础方法；如项目迭代久了，形成多个版本时，可参考该接口对增删改查的定义，
 * 修改不同版本的方法名，推荐方法名后加 `Vn` 其中 `n`为版本号
 *
 * @param <RB> 资源类型
 * @param <ID> 资源Bean的主键ID类型
 */
public interface StandardResourceService<RB, ID> {

  /**
   * 创建资源记录
   */
  default Result create(RB resource) {
    // will be implemented
    return Results.success();
  }

  /**
   * 修改资源记录
   */
  default Result edit(ID id, RB resource) {
    // will be implemented
    return Results.success();
  }

  /**
   * 删除资源记录
   */
  default Result remove(ID id) {
    // will be implemented
    return Results.success();
  }

  /**
   * 删除资源记录
   */
  default Result removes(ID[] ids) {
    // will be implemented
    return Results.success();
  }

  /**
   * 获取指定id的资源记录
   */
  default Result info(ID id) {
    // will be implemented
    return Results.success();
  }

  /**
   * 根据条件搜索资源记录
   */
  default Result find(RB resource) {
    // will be implemented
    return Results.success();
  }
}
