package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口或方法上，对API响应数据进行加密
 * <p>
 * todo 待实现
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Crypto {
}
