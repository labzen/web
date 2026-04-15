package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 异常捕获处理注解。
 * <p>
 * 注解在 Controller 接口或方法上，用于处理特定异常时的定制响应数据。
 * <p>
 * Labzen Web 组件已对所有异常进行统一封装，默认返回统一的响应数据结构。
 * 声明本注解后，可以根据抛出的异常类型，返回有意义的响应数据。
 * <p>
 * <b>待实现：</b> 改为接口方式，定义支持的异常类型及对应的处理逻辑。
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Catching {

  // todo 改为接口，在接口中定义支持哪些异常，并处理返回数据
  Class<?>[] exceptions();
}
