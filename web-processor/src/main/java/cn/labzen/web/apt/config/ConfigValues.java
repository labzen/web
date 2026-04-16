package cn.labzen.web.apt.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * APT 处理器配置的默认值
 * <p>
 * 定义所有配置项的默认值，当 labzen.web.config 中未设置对应项时使用。
 */
@Getter
@RequiredArgsConstructor
public enum ConfigValues {

  CLASS_NAME_SUFFIX_VALUE("Impl"),
  API_VERSION_CARRIER_VALUE("HEADER"),
  API_VERSION_PREFIX_VALUE("v"),
  API_VERSION_BASED_VALUE("1"),
  API_VERSION_HEADER_VND_VALUE("app"),
  API_VERSION_PARAMETER_NAME_VALUE("version");

  private final String value;
}
