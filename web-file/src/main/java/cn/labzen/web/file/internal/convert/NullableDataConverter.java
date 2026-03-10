package cn.labzen.web.file.internal.convert;

import org.springframework.beans.SimpleTypeConverter;

public class NullableDataConverter implements DataConverter<Object, Object> {

  private static final SimpleTypeConverter CONVERTER = new SimpleTypeConverter();

  private final Object defaultValue;

  public NullableDataConverter(Class<?> type, Object defaultValue) {
    this.defaultValue = CONVERTER.convertIfNecessary(defaultValue, type);
  }

  @Override
  public Object convert(Object source) {
    return source == null ? defaultValue : source;
  }

  /**
   * 无法还原
   */
  @Override
  public Object restore(Object target) {
    return target;
  }
}
