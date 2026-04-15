package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * API 响应数据加密注解。
 * <p>
 * 注解在 Controller 接口或方法上，对 API 响应数据进行加密处理。
 * <p>
 * <b>待实现：</b> 定义加密算法、密钥配置等参数。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Crypto {
}
