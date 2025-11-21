package cn.labzen.web.meta;

import cn.labzen.meta.configuration.annotation.Configured;
import cn.labzen.meta.configuration.annotation.Item;
import cn.labzen.web.defination.APIVersionCarrier;

@Configured(namespace = "web")
public interface WebConfiguration {

  /**
   * todo 考虑个合适的方式，在生成代码的时候，通过某个合适途径，将这个值传递给runtime环境，而不是两边配置<br/>
   * API 版本控制，version 的位置，支持 HEADER（默认）, URI, PARAM
   * <p>
   * 默认使用 HEADER，这里需要***注意***，如果在 `labzen.web.config` 中修改了 `processor.api-version.carrier` 配置，`labzen.yml` 中这个配置也要相应修改为一致的值
   * <p>
   * <li>DISABLE - 禁用版本控制！
   * <li>HEADER - 通过请求头部信息 Accept: 来传递请求 API 的版本信息，例如：'Accept: application/vnd.app.v1+json'
   * <li>URI - 通过 API 的请求地址前置版本信息，例如 'https://www.app.com/v1/login'
   * <li>PARAMETER - 通过请求 API 时，使用参数来传递版本信息，例如 'https://www.app.com/login?version=v1'
   */
  @Item(path = "api-version.carrier", required = false, defaultValue = "HEADER")
  APIVersionCarrier apiVersionCarrier();

  /**
   * API 版本控制的版本前缀，默认小写 v
   */
  @Item(path = "api-version.prefix", required = false, defaultValue = "v")
  String apiVersionPrefix();

  /**
   * todo 再考虑考虑，这个是不是真的有必要<br/>
   * API 版本控制强制要求访问API时带有Accept Header信息，默认false，如果访问API的Header种没有Accept，
   * Spring默认会选择一个可以匹配的 produces 方法进行响应，当 api-version.carrier 为 HEADER 时有效
   */
  @Deprecated
  @Item(path = "api-version.header-accept-forced", required = false, defaultValue = "true")
  boolean apiVersionHeaderAcceptForced();

  /**
   * 定义统一的 API 路径前缀，默认：/api；可根据项目实际情况自定义，如不需要前缀则将该值设置为空字符
   */
  @Item(path = "api-path-prefix", required = false, defaultValue = "api")
  String apiPathPrefix();

  /**
   * 启用 Response 响应内容统一格式化，默认 true，否则使用 Spring 默认的格式化
   */
  @Item(path = "response.formatting.enable", required = false, defaultValue = "true")
  boolean responseFormattingEnabled();

  /**
   * 默认会格式化所有返回类型为 [Result] 的响应数据。如果设置为 true，则会对所有的 Controller 返回类型进行格式化
   */
  @Item(path = "response.formatting.all-forced", required = false, defaultValue = "true")
  boolean responseFormattingForcedAll();

  /**
   * 设置一个对 [Pageable] 和 [Pagination] 的转换器（实现 [PageConverter]）类的 FQCN。将会尝试先从 Spring 容器中寻找是否有注册过的组件对象，如果不存在则创建一个实例
   */
  @Item(path = "page-converter", required = false, defaultValue = "")
  String pageConverter();

  /**
   * 设置默认分页大小，默认20
   */
  @Item(path = "page-size", required = false, defaultValue = "20")
  int pageSize();
}
