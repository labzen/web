package cn.labzen.web.ap.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
