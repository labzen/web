package cn.labzen.web.file.internal.convert;

import cn.labzen.tool.util.Strings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StringSuffixConverter extends AbstractStringConverter {

  private final String suffix;

  @Override
  public String convert(String source) {
    return Strings.value(source, "") + suffix;
  }

  @Override
  public String restore(String target) {
    return Strings.trim(target, suffix, Strings.Position.RIGHT);
  }
}
