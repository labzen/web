package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * API 响应缓存注解。
 * <p>
 * 注解在 Controller 接口或方法上，对 API 响应内容进行缓存处理。
 * <p>
 * <b>待实现：</b> 定义缓存参数，如缓存时长、缓存策略等。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Caching {
}
