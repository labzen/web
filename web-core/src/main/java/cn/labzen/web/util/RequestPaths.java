package cn.labzen.web.util;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.meta.WebCoreConfiguration;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 请求路径工具类，负责组装和提供 API 请求的统一前缀及完整路径。
 * <p>
 * 支持两种 API 版本携带方式：
 * <ul>
 *   <li>{@link cn.labzen.web.api.definition.APIVersionCarrier#URI} - 版本号通过 URI 路径传递，如 /api/v1/users</li>
 *   <li>{@link cn.labzen.web.api.definition.APIVersionCarrier#HEADER} - 版本号通过请求头传递，如 X-API-Version: v1</li>
 * </ul>
 * <p>
 * 当使用 URI 方式时，会自动生成版本号通配符前缀（如 v*），以便匹配多个版本。
 *
 * @author labzen
 * @since 1.0.0
 */
public final class RequestPaths {

  /**
   * 缓存的 API 前缀，在类加载时初始化，之后复用
   */
  private static final String CACHED_PREFIX;

  static {
    CACHED_PREFIX = buildPrefix();
  }

  /**
   * 私有构造函数，防止实例化
   */
  private RequestPaths() {
    throw new UnsupportedOperationException("工具类不允许实例化");
  }

  /**
   * 拼接一个请求地址，包含统一前缀，以及版本号通配符（如果版本使用路径控制的话，api-version.carrier=URI）
   *
   * @param path API 路径，不应以 / 开头
   * @return 完整的请求地址，以 / 开头
   * @throws IllegalArgumentException 如果 path 为空
   */
  public static String assemble(String path) {
    if (Strings.isBlank(path)) {
      throw new IllegalArgumentException("请求路径不能为空");
    }
    return CACHED_PREFIX + Strings.insureStartsWith(path, "/");
  }

  /**
   * 构建 API 统一前缀，包含版本号通配符（如果版本使用路径控制的话，api-version.carrier=URI）
   *
   * @return 构建后的前缀
   */
  private static String buildPrefix() {
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
