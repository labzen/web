package cn.labzen.web.file.annotation;

import cn.labzen.web.file.definition.enums.Alignment;
import cn.labzen.web.file.definition.enums.BorderWidth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static cn.labzen.web.file.definition.bean.Style.*;

/**
 * 以行为单位单独定义内容单元格的样式
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DataStyle {

  /**
   * 行中单元格对齐方式，如果要指定，必须为2个元素，否则不生效，第一个元素为水平对齐方式，第二个元素为垂直对齐方式
   */
  Alignment[] align() default {Alignment.CENTER, Alignment.CENTER};

  /**
   * 行中单元格背景色
   */
  String backgroundColor() default DEFAULT_BACKGROUND_COLOR;

  /**
   * 行中单元格字体大小，对应Excel中的字体高度
   */
  short fontSize() default DEFAULT_FONT_SIZE;

  /**
   * 行中单元格字体颜色
   */
  String fontColor() default DEFAULT_FONT_COLOR;

  /**
   * 行中单元格字体是否加粗
   */
  boolean fontBold() default DEFAULT_FONT_BOLD;

  /**
   * 行中单元格边框宽度
   */
  BorderWidth borderWidth() default BorderWidth.THIN;

  /**
   * 行中单元格是否自动换行
   */
  boolean wrapped() default DEFAULT_WRAPPED;

  /**
   * 行中单元格是否隐藏
   */
  boolean hidden() default DEFAULT_HIDDEN;
}
