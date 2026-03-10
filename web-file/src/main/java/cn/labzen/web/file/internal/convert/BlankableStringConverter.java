package cn.labzen.web.file.internal.convert;

import cn.labzen.tool.util.Strings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlankableStringConverter extends AbstractStringConverter {

  private final String defaultValue;

  @Override
  public String convert(String source) {
    return Strings.valueWhenBlank(source, defaultValue);
  }

  /**
   * 无法还原
   */
  @Override
  public String restore(String target) {
    return target;
  }
}
