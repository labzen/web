package cn.labzen.web.api.annotation.runtime;

import cn.labzen.web.api.definition.APIVersionCarrier;

import java.lang.annotation.*;

/**
 * 标识一个 Controller 方法的 API 请求版本，仅在使用 {@link APIVersionCarrier#URI} 方式控制 API 版本的情况下有效
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface APIVersion {

  String value();
}
