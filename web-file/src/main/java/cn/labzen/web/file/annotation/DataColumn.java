package cn.labzen.web.file.annotation;

import cn.labzen.web.file.definition.enums.NoneRefer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static cn.labzen.web.file.definition.bean.Column.DEFAULT_COLUMN_BLANKABLE_HINT;
import static cn.labzen.web.file.definition.bean.Column.DEFAULT_COLUMN_NULLABLE_HINT;

/**
 * 为需要导出到文件的属性定义列信息
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataColumn {

  /**
   * 列头标题，不填写默认使用属性名
   */
  String[] header() default {""};

  /**
   * 列索引，从0开始，越小越靠左显示，不填写默认使用类中定义属性的顺序
   * <p>
   * 假如同一个类中，只定义了某几个属性的index，则最终顺序会先确定明确定义index的列，其他的属性再按照类中定义的顺序进行排序
   * <p>
   * 例如：属性 a,b,c,d,e,f；只有c和e指定了index分别为0和2，则最终列的顺序为c,a,e,b,d,f
   */
  int index() default -1;

  /**
   * 列宽度，不填写使用默认宽度
   */
  int width() default -1;

  /**
   * 忽略此列的信息导出，默认为false
   */
  boolean ignore() default false;

  /**
   * 日期格式，只对{@link java.util.Date} {@link java.time.LocalDateTime}等日期类型有效，默认为yyyy-MM-dd HH:mm:ss
   */
  String datePattern() default "yyyy-MM-dd HH:mm:ss";

  /**
   * 数值格式，只对{@link Float} {@link Double}等浮点类型有效，默认为#.##
   */
  String decimalsPattern() default "#.##";

  /**
   * todo 前缀，在输出的最终值（经过各种转换后的呈现文字）前添加，默认为空
   */
  String prefix() default "";

  /**
   * todo 后缀，在输出的最终值（经过各种转换后的呈现文字）后添加，默认为空
   */
  String suffix() default "";

  /**
   * todo 默认值，当数据为null时，使用此默认值，可以为任何能够转换为目标简单类型的字符串，例如数字字符，布尔字符串
   */
  String defaultWhenNull() default DEFAULT_COLUMN_NULLABLE_HINT;

  /**
   * todo 默认值，当数据为空字符串时，使用此默认值，仅对{@link String}类型有效，默认为空
   */
  String defaultWhenBlank() default DEFAULT_COLUMN_BLANKABLE_HINT;

  /**
   * todo 参考枚举类来转换数据值，一般适合使用枚举来定义字典值的情况
   */
  Class<? extends Enum<?>> referEnum() default NoneRefer.class;

  /**
   * todo 仅当{@link #referEnum}有值时才有效，指定使用枚举的哪个方法获取字典值，默认转换数据值使用枚举的 toString() 方法
   */
  String referEnumMethod() default "toString";

  /**
   * todo 参考使用等号定义的字典值来转换数据值，优先会使用{@link #referEnum()}，格式为"原数据=转换后的文字"，多个等式使用英文逗号分隔，如果格式不正确则不生效
   */
  String referEquationString() default "";
}
