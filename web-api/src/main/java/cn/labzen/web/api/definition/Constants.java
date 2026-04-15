package cn.labzen.web.api.definition;

/**
 * 常量定义接口。
 * <p>
 * 定义了 Labzen Web 框架使用的各类常量，包括请求属性键、日志场景等。
 */
public interface Constants {

  /**
   * 请求时间属性键（格式：yyyy-MM-dd HH:mm:ss）
   */
  String REST_REQUEST_TIME = "labzen.runtime.web.request.time";

  /**
   * 请求时间属性键（毫秒）
   */
  String REST_REQUEST_TIME_MILLIS = "labzen.runtime.web.request.time.millis";

  /**
   * 请求执行时间属性键（毫秒）
   */
  String REST_EXECUTION_TIME = "labzen.runtime.web.execution.time";

  /**
   * Controller 日志场景标识
   */
  String LOGGER_SCENE_CONTROLLER = "Controller";

  /**
   * 请求异常已记录属性键
   */
  String EXCEPTION_WAS_LOGGED_DURING_REQUEST = "labzen.request.exception.logged";

  /**
   * 默认分页页码
   */
  int DEFAULT_PAGE_NUMBER = 1;
}
