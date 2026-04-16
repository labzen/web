package cn.labzen.web.apt.config;

import cn.labzen.web.apt.LabzenWebProcessor;
import com.google.common.primitives.Ints;

import java.util.Optional;
import java.util.Properties;

import static cn.labzen.web.apt.config.ConfigKeys.*;
import static cn.labzen.web.apt.config.ConfigValues.*;

/**
 * APT 注解处理器配置类
 * <p>
 * 负责从 labzen.web.config 中读取注解处理器的各项配置，提供类型安全的配置值访问方法。
 * 如果配置项未设置或为空，则返回默认值。
 */
public class Config {

  private final Properties properties;

  /**
   * 构造配置对象
   *
   * @param properties 配置属性集合
   */
  public Config(Properties properties) {
    this.properties = properties;
  }

  /**
   * 获取生成的 Controller 实现类名后缀
   *
   * @return 类名后缀，默认 "Impl"
   */
  public String classNameSuffix() {
    var original = properties.getOrDefault(CLASS_NAME_SUFFIX.getValue(), CLASS_NAME_SUFFIX_VALUE.getValue()).toString();
    return original.isBlank() ? CLASS_NAME_SUFFIX_VALUE.getValue() : original;
  }

  /**
   * 获取 API 版本信息的携带方式
   *
   * @return 版本携带方式字符串，默认 "HEADER"
   */
  public String apiVersionCarrier() {
    var carrier = properties.getOrDefault(API_VERSION_CARRIER.getValue(), API_VERSION_CARRIER_VALUE.getValue()).toString();
    return carrier.isBlank() ? API_VERSION_CARRIER_VALUE.getValue() : carrier;
  }

  /**
   * 获取 API 版本号前缀
   *
   * @return 版本前缀，默认 "v"
   */
  public String apiVersionPrefix() {
    var original = properties.getOrDefault(API_VERSION_PREFIX.getValue(), API_VERSION_PREFIX_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_PREFIX_VALUE.getValue() : original;
  }

  /**
   * 获取 API 版本基础号
   *
   * @return 基础版本号，默认 1
   */
  public int apiVersionBased() {
    var based = properties.getOrDefault(API_VERSION_BASED.getValue(), API_VERSION_BASED_VALUE.getValue()).toString();
    return Optional.ofNullable(Ints.tryParse(based)).orElseGet(() -> {
      LabzenWebProcessor.getContext().messaging().warning("No valid processor.api-version.based is configured, 1 is used by default");
      return 1;
    });
  }

  /**
   * 获取 Accept Header 中的 vendor 名称
   * <p>
   * 用于构建 application/vnd.{vendor}.v{x}+json
   *
   * @return vendor 名称，默认 "app"
   */
  public String apiVersionHeaderVND() {
    var original = properties.getOrDefault(API_VERSION_HEADER_VND.getValue(), API_VERSION_HEADER_VND_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_HEADER_VND_VALUE.getValue() : original;
  }

  /**
   * 获取通过请求参数传递版本号时的参数名称
   *
   * @return 参数名称，默认 "version"
   */
  public String apiVersionParameterName() {
    var original = properties.getOrDefault(API_VERSION_PARAMETER_NAME.getValue(), API_VERSION_PARAMETER_NAME_VALUE.getValue()).toString();
    return original.isBlank() ? API_VERSION_PARAMETER_NAME_VALUE.getValue() : original;
  }
}
