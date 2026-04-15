package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * API 并发量阈值注解。
 * <p>
 * 注解在 Controller 接口方法上，用于调整接口的并发访问量域值相关参数。
 * 可配置限流阈值、熔断策略等。
 * <p>
 * <b>待实现：</b> 定义限流参数、熔断策略等配置。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Threshold {
}
