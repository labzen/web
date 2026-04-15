package cn.labzen.web.api.controller;

/**
 * 简化版 Controller 接口。
 * <p>
 * 用于没有明确（或单一）资源的业务场景。继承本接口后，方法的定义以及注解的使用方式，
 * 可参考 {@link StandardController}。
 * <p>
 * <b>泛型参数说明：</b>
 * <ul>
 *   <li>BS: 业务服务组件，指定当前 Controller 入口需要调用的业务逻辑处理类（通常为 XXXService）</li>
 * </ul>
 *
 * @param <BS> 业务服务组件类
 */
public interface SimplestController<BS> extends LabzenController {
}
