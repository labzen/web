package cn.labzen.web.util;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.meta.WebCoreConfiguration;
import com.google.common.collect.Lists;

import java.util.List;

public final class RequestPaths {

  /**
   * 拼接一个请求地址，包含统一前缀，以及版本号通配符（如果版本使用路径控制的话，api-version.carrier=URI）
   */
  public static String assemble(String path) {
    String prefix = getPrefix();
    return prefix + Strings.insureStartsWith(path, "/");
  }

  /**
   * 获取API统一前缀，包含版本号通配符（如果版本使用路径控制的话，api-version.carrier=URI）
   */
  public static String getPrefix() {
    WebCoreConfiguration webCoreConfiguration = Labzens.configurationWith(WebCoreConfiguration.class);
    List<String> prefixChips = Lists.newArrayList();

    // 统一的 API 前缀
    if (Strings.isNotBlank(webCoreConfiguration.apiPathPrefix())) {
      prefixChips.add(Strings.trim(webCoreConfiguration.apiPathPrefix(), "/"));
    }
    if (webCoreConfiguration.apiVersionCarrier() == APIVersionCarrier.URI) {
      // URI 的版本号前缀默认为 v。v* 匹配 v1, v2 等版本
      prefixChips.add(webCoreConfiguration.apiVersionPrefix() + "*");
    }

    return "/" + String.join("/", prefixChips);
  }
}
