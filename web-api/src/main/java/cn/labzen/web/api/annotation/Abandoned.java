package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 标识 Controller 接口方法已弃用的注解。
 * <p>
 * 使用此注解标记的接口方法将不再作为增删改查的 Restful API 入口。
 * 生成的 Controller 实现类中，该方法将被排除在外。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Abandoned {
}
