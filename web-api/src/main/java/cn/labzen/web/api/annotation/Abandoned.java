package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 注解在 Controller 接口方法上，标识该方法弃用，将不再作为增删改查的 Restful API 入口
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Abandoned {
}
