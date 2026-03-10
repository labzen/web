package cn.labzen.web.file.internal.convert;

import cn.labzen.tool.util.Strings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StringPrefixConverter extends AbstractStringConverter {

  private final String prefix;

  @Override
  public String convert(String source) {
    return prefix + Strings.value(source, "");
  }

  @Override
  public String restore(String target) {
    return Strings.trim(target, prefix, Strings.Position.LEFT);
  }
}
