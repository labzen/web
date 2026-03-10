package cn.labzen.web.file.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对当前类的样式做全局定义，或对某个属性做表头和内容的样式定义
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataStyles {

  /**
   * 表头样式
   */
  DataStyle header() default @DataStyle;

  /**
   * 内容样式，在每个字段上使用{@link DataStyle}单独定义样式时，会覆盖全局样式
   */
  DataStyle content() default @DataStyle;
}
