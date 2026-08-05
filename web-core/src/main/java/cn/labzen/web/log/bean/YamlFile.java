package cn.labzen.web.log.bean;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;

import java.util.Map;

/**
 * YAML 文件顶层映射结构。
 * <p>
 * 仅用于 SnakeYAML 反序列化，不作为公开 API。
 * general → {@link ApiLogConfig}，methods → Map&lt;方法Key, {@link ApiEndpointLogConfig}&gt;。
 */
public class YamlFile {

  public ApiLogConfig general;
  public Map<String, ApiEndpointLogConfig> methods;
}
