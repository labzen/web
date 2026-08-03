package cn.labzen.web.api.definition;

/**
 * 常量定义接口。
 * <p>
 * 定义了 Labzen Web 框架使用的各类常量，包括请求属性键、日志场景等。
 */
public final class Constants {

  private Constants() {
  }

  /**
   * 请求时间属性键（格式：yyyy-MM-dd HH:mm:ss）
   */
  public static final String REST_REQUEST_TIME = "labzen.runtime.web.request.time";

  /**
   * 请求时间属性键（毫秒）
   */
  public static final String REST_REQUEST_TIME_MILLIS = "labzen.runtime.web.request.time.millis";

  /**
   * 请求执行时间属性键（毫秒）
   */
  public static final String REST_EXECUTION_TIME = "labzen.runtime.web.execution.time";

  /**
   * Controller 日志场景标识
   */
  public static final String LOGGER_SCENE_CONTROLLER = "Controller";

  /**
   * API 日志场景标识，用于 LabzenLogger 结构化日志的 .scene() 调用
   */
  public static final String LOGGER_SCENE_API_LOG = "API-LOG";

  /**
   * 请求异常已记录属性键
   */
  public static final String EXCEPTION_WAS_LOGGED_DURING_REQUEST = "labzen.request.exception.logged";

  /**
   * API 日志配置缓存属性键，用于在请求处理链（Interceptor → Controller → ResponseAdvice）中传递日志配置
   */
  public static final String API_LOG_CONFIG_ATTRIBUTE = "labzen.runtime.web.api.log-config";

  /**
   * API 日志控制器元数据属性键，用于在请求处理链中传递 ControllerMeta
   */
  public static final String API_LOG_CONTROLLER_META_ATTRIBUTE = "labzen.runtime.web.api.meta";

  /**
   * API 日志匹配条件属性键，用于标记当前请求是否匹配了某条条件日志规则
   */
  public static final String API_LOG_MATCHED_CONDITION_ATTRIBUTE = "labzen.api.log.matched.condition";

  /**
   * classpath 下 API 日志 YAML 配置文件的存放目录
   */
  public static final String API_LOG_CONFIG_DIR = "labzen-web";

  /**
   * 全局配置 key，用于 programmaticConfigs 中表示全局默认配置
   */
  public static final String API_LOG_KEY_GLOBAL = "__global__";

  /**
   * 通用配置 key，用于 Controller 级配置 Map 中表示 general 通用配置
   */
  public static final String API_LOG_KEY_GENERAL = "__general__";

  /**
   * 默认分页页码
   */
  public static final int DEFAULT_PAGE_NUMBER = 1;
}
