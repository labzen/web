package cn.labzen.web.meta;

import cn.labzen.meta.configuration.annotation.Configured;
import cn.labzen.meta.configuration.annotation.Item;
import cn.labzen.web.api.definition.APIVersionCarrier;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.request.StandardUploadedFile;

import java.util.List;

@Configured(namespace = "web")
public interface WebCoreConfiguration {

  /**
   * 是否开启 Debug 模式，默认 false。在开发阶段使用。
   * <p>
   * 开启后，实现了 {@link Pageable} 接口的 Bean，在调试窗口中可以直接看到参数值，否则参数值看到的全是默认值（但不影响其实际保存的值）
   */
  @Item(path = "debug", required = false, defaultValue = "false")
  boolean debug();

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
  @Item(path = "core.api-version.carrier", required = false, defaultValue = "HEADER")
  APIVersionCarrier apiVersionCarrier();

  /**
   * API 版本控制的版本前缀，默认小写 v
   */
  @Item(path = "core.api-version.prefix", required = false, defaultValue = "v")
  String apiVersionPrefix();

  /**
   * todo 再考虑考虑，这个是不是真的有必要<br/>
   * API 版本控制强制要求访问API时带有Accept Header信息，默认false，如果访问API的Header种没有Accept，
   * Spring默认会选择一个可以匹配的 produces 方法进行响应，当 api-version.carrier 为 HEADER 时有效
   */
  @Deprecated
  @Item(path = "core.api-version.header-accept-forced", required = false, defaultValue = "true")
  boolean apiVersionHeaderAcceptForced();

  /**
   * 定义统一的 API 路径前缀，默认：/api；可根据项目实际情况自定义，如不需要前缀则将该值设置为空字符
   */
  @Item(path = "core.api-path-prefix", required = false, defaultValue = "api")
  String apiPathPrefix();

  /**
   * 启用 Response 响应内容统一格式化，默认 true，否则使用 Spring 默认的格式化
   */
  @Item(path = "core.response.formatting.enable", required = false, defaultValue = "true")
  boolean responseFormattingEnabled();

  /**
   * 默认会格式化所有返回类型为 [Result] 的响应数据。如果设置为 true，则会对所有的 Controller 返回类型进行格式化
   */
  @Item(path = "core.response.formatting.all-forced", required = false, defaultValue = "true")
  boolean responseFormattingForcedAll();

  /**
   * 设置一个对 [Pageable] 和 [Pagination] 的转换器（实现 [PageConverter]）类的 FQCN。将会尝试先从 Spring 容器中寻找是否有注册过的组件对象，如果不存在则创建一个实例
   */
  @Item(path = "core.page-converter", required = false, defaultValue = "")
  String pageConverter();

  /**
   * 设置默认分页大小，默认20
   */
  @Item(path = "core.page-size", required = false, defaultValue = "20")
  int pageSize();

  /**
   * 最大分页大小，默认100。防止用户恶意传入极大的 pageSize（如 99999999），而导致 OOM 或数据库性能问题
   */
  @Item(path = "core.max-page-size", required = false, defaultValue = "100")
  int maxPageSize();

  // ===================================================================================================================

  @Item(path = "file.upload.default-storage", required = false, defaultValue = "LocalFileStorage")
  String defaultFileStorage();

  /**
   * 允许上传的文件扩展名
   * TODO 既然是通过 {@link StandardUploadedFile} 来使用这个配置限制文件上传，比如给这个类开一个配置方法，而不是全局的配置文件中来设置
   */
  @Item(path = "file.upload.accept-extension", required = false, defaultValue = "xlsx,png,jpg,jpeg,bmp")
  List<String> acceptedUploadFileExtensions();

  /**
   * 临时文件清理间隔时间（单位：秒），默认一小时清理一次，每次清理会将上一次清理时间之前的所有文件删除掉
   */
  @Item(path = "file.upload.temp-cleanup-interval", required = false, defaultValue = "3600")
  long tempFileCleanupInterval();

  // ===================================================================================================================
  // API 日志配置
  // ===================================================================================================================

  /**
   * API 日志全局开关，默认关闭，可根据业务需要，针对某些特定API单独打开。
   * <p>
   * 开启后，所有 Controller 方法的请求和响应日志将按照各接口的配置进行打印。
   * 建议在开发环境开启，生产环境按需临时开启排查问题。
   */
  @Item(path = "log.enabled", required = false, defaultValue = "false")
  boolean apiLogEnabled();

  /**
   * API 日志全局默认级别，默认 DEBUG。
   * <p>
   * 可选值：TRACE、DEBUG、INFO、WARN、ERROR。
   * 可通过 YAML 或程序化 API 按 Controller/方法级别覆盖。
   */
  @Item(path = "log.level", required = false, defaultValue = "DEBUG")
  String apiLogLevel();

  /**
   * API 日志全局默认采样率，范围 [0.0, 1.0]，默认 1.0（全量）。
   * <p>
   * 用于防止高 QPS 接口的日志风暴。设置为 0.1 表示仅 10% 的请求打印日志。
   */
  @Item(path = "log.sampling-rate", required = false, defaultValue = "1.0")
  double apiLogSamplingRate();

  /**
   * 是否输出请求日志，默认 true
   */
  @Item(path = "log.out-request", required = false, defaultValue = "true")
  boolean apiLogRequest();

  /**
   * 是否输出响应日志，默认 false
   */
  @Item(path = "log.out-response", required = false, defaultValue = "false")
  boolean apiLogResponse();
}
