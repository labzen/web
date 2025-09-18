package cn.labzen.web.ap.config;

import cn.labzen.web.defination.APIVersionCarrier;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Properties;

import static cn.labzen.web.ap.config.ConfigKeys.*;
import static cn.labzen.web.ap.config.ConfigValues.*;

@Slf4j
public class Config {

  private final Properties properties;

  public Config(Properties properties) {
    this.properties = properties;
  }

  public String classNameSuffix() {
    var original = properties.getOrDefault(CLASS_NAME_SUFFIX.getValue(), CLASS_NAME_SUFFIX_VALUE.getValue()).toString();
    return original.isBlank() ? CLASS_NAME_SUFFIX_VALUE.getValue() : original;
  }

  public APIVersionCarrier apiVersionCarrier() {
    var carrier = properties.getOrDefault(API_VERSION_CARRIER.getValue(), API_VERSION_CARRIER_VALUE.getValue()).toString();
    try {
      return APIVersionCarrier.valueOf(carrier);
    } catch (IllegalArgumentException e) {
      logger.warn("未配置有效的 processor.api-version.carrier ，将使用 DISABLE，生成的 Controller 实现类将禁用API版本控制能力");
      return APIVersionCarrier.DISABLE;
    }
  }

  public String apiVersionPrefix() {
    var original = properties.getOrDefault(API_VERSION_PREFIX.getValue(), API_VERSION_PREFIX_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_PREFIX_VALUE.getValue() : original;
  }

  public int apiVersionBased() {
    var based = properties.getOrDefault(API_VERSION_BASED.getValue(), API_VERSION_BASED_VALUE.getValue()).toString();
    return Optional.ofNullable(Ints.tryParse(based)).orElseGet(() -> {
      logger.warn("未配置有效的 processor.api-version.based ，默认使用 1");
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
