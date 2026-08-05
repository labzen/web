package cn.labzen.web.log.bean;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ApiLogConfig;
import lombok.Data;

import java.util.Map;

/**
 * YAML 文件顶层映射结构。
 * <p>
 * 仅用于 SnakeYAML 反序列化，不作为公开 API。
 * general → {@link ApiLogConfig}，methods → Map&lt;方法Key, {@link ApiEndpointLogConfig}&gt;。
 */
@Data
public class YamlFile {

  private ApiLogConfig general;
  private Map<String, ApiEndpointLogConfig> methods;
}
