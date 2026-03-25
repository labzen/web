package cn.labzen.web.api.response.result;

import cn.labzen.tool.util.Strings;
import jakarta.annotation.Nonnull;

import java.io.File;

public record FileResult(String filename, @Nonnull File value) implements Result {

  private static final String DEFAULT_FILE_NAME = "Unknown";

  public FileResult(File value) {
    this(null, value);
  }

  @Override
  public int code() {
    return 200;
  }

  @Override
  public String message() {
    return "";
  }

  public String filename() {
    return Strings.value(filename, Strings.value(value.getName(), DEFAULT_FILE_NAME));
  }
}
