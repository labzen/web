package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口方法上，调整接口的并发量域值相关参数
 * <p>
 * todo 待实现
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Threshold {
}
