package cn.labzen.web.meta;

import cn.labzen.web.api.paging.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * 单元测试用的 Web 组件配置桩。
 * <p>
 * 分页组件的静态初始化依赖 {@code Labzens} 配置，这里直接向配置代理缓存注入一个默认实现，
 * 使不启动 Spring 容器的测试也能使用分页解析与代理能力。
 */
public final class WebCoreConfigurationStub {

  private WebCoreConfigurationStub() {
  }

  /**
   * 注册配置桩；容器中已存在真实配置时不覆盖
   */
  @SuppressWarnings("unchecked")
  public static void install() throws Exception {
    Class<?> processor = Class.forName("cn.labzen.meta.configuration.ConfigurationProcessor");
    Field proxiesField = processor.getDeclaredField("PROXIES");
    proxiesField.setAccessible(true);
    Map<Class<?>, Object> proxies = (Map<Class<?>, Object>) proxiesField.get(null);
    proxies.putIfAbsent(WebCoreConfiguration.class, instance());
  }

  public static WebCoreConfiguration instance() {
    return (WebCoreConfiguration) Proxy.newProxyInstance(
        WebCoreConfiguration.class.getClassLoader(),
        new Class<?>[]{WebCoreConfiguration.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "debug" -> false;
          case "pageSize" -> Pageable.DEFAULT_PAGE_SIZE;
          case "maxPageSize" -> 100;
          case "pageConverter" -> "";
          case "apiVersionPrefix" -> "v";
          case "apiPathPrefix" -> "api";
          case "responseFormattingEnabled", "responseFormattingForcedAll" -> true;
          case "apiLogLevel" -> "DEBUG";
          case "apiLogSamplingRate" -> 1.0;
          case "acceptedUploadFileExtensions" -> List.of("xlsx");
          case "toString" -> "WebCoreConfigurationStub";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0.0;
    }
    if (type == String.class) {
      return "";
    }
    return null;
  }
}
