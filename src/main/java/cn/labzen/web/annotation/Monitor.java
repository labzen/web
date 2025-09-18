package cn.labzen.web.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口或方法上，对指定的API进行指标监控
 * <p>
 * todo 待实现
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Monitor {
}
