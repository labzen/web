package cn.labzen.web.api.controller;

/**
 * 继承本接口，为了明确Restful入口可调用的业务逻辑处理类组件
 * <p>
 * 这个接口的目的是为了没有明确（或单一）资源的业务场景提供的；继承本接口后，方法的定义以及方法注解的使用，可参考 {@link StandardController}
 * <p>
 * <b>A.</b> 本接口的泛型定义：
 * <li> BS: Business Service Component - 指定当前Controller入口需要调用的业务逻辑处理类，一般为XXXService
 *
 * @param <BS> Business Service Component
 */
public interface SimplestController<BS> extends LabzenController {
}
