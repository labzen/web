package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口或方法上，对API响应内容进行缓存
 * <p>
 * todo 待实现，定义缓存参数
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Caching {
}
