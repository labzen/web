package cn.labzen.web.apt.config;

import cn.labzen.web.apt.LabzenWebProcessor;
import com.google.common.primitives.Ints;

import java.util.Optional;
import java.util.Properties;

import static cn.labzen.web.apt.config.ConfigKeys.*;
import static cn.labzen.web.apt.config.ConfigValues.*;

public class Config {

  private final Properties properties;

  public Config(Properties properties) {
    this.properties = properties;
  }

  public String classNameSuffix() {
    var original = properties.getOrDefault(CLASS_NAME_SUFFIX.getValue(), CLASS_NAME_SUFFIX_VALUE.getValue()).toString();
    return original.isBlank() ? CLASS_NAME_SUFFIX_VALUE.getValue() : original;
  }

  public String apiVersionCarrier() {
    var carrier = properties.getOrDefault(API_VERSION_CARRIER.getValue(), API_VERSION_CARRIER_VALUE.getValue()).toString();
    return carrier.isBlank() ? API_VERSION_CARRIER_VALUE.getValue() : carrier;
  }

  /**
   * 有默认值，不可能为空字符串
   */
  public String apiVersionPrefix() {
    var original = properties.getOrDefault(API_VERSION_PREFIX.getValue(), API_VERSION_PREFIX_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_PREFIX_VALUE.getValue() : original;
  }

  public int apiVersionBased() {
    var based = properties.getOrDefault(API_VERSION_BASED.getValue(), API_VERSION_BASED_VALUE.getValue()).toString();
    return Optional.ofNullable(Ints.tryParse(based)).orElseGet(() -> {
      LabzenWebProcessor.getContext().messaging().warning("No valid processor.api-version.based is configured, 1 is used by default");
      return 1;
    });
  }

  public String apiVersionHeaderVND() {
    var original = properties.getOrDefault(API_VERSION_HEADER_VND.getValue(), API_VERSION_HEADER_VND_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_HEADER_VND_VALUE.getValue() : original;
  }

  public String apiVersionParameterName() {
    var original = properties.getOrDefault(API_VERSION_PARAMETER_NAME.getValue(), API_VERSION_PARAMETER_NAME_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_PARAMETER_NAME_VALUE.getValue() : original;
  }
}
