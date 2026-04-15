package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * API 监控指标注解。
 * <p>
 * 注解在 Controller 接口或方法上，用于对指定的 API 进行指标监控。
 * <p>
 * <b>待实现：</b> 定义监控项、监控指标阈值等参数。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Monitor {
}
