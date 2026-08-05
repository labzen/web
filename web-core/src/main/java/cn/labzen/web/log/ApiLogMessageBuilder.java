package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Scenes;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.HttpStatusExt;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.api.response.result.FileResult;
import cn.labzen.web.log.bean.MultipartDetail;
import cn.labzen.web.util.ControllerDisposeHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static cn.labzen.web.api.definition.Constants.API_CONTROLLER_META_ATTRIBUTE;

/**
 * API 日志消息构建工具。
 * <p>
 * 负责构建结构化 API 日志消息，使用 {@link LabzenLogger} 的结构化 API 输出日志。
 * 核心功能：
 * <ul>
 *   <li><b>参数提取</b>：从请求中提取 query string / form / multipart / JSON body 参数</li>
 *   <li><b>参数过滤</b>：按配置的 {@code includeParams}（白名单）或 {@code excludeParams}（黑名单）过滤请求参数</li>
 *   <li><b>文件处理</b>：识别上传文件类型（{@link MultipartFile}/{@link Part}）打印元信息而非二进制内容</li>
 * </ul>
 * <p>
 * <b>职责边界：</b>响应体的预处理（文件类型检测、截断）由 {@link ApiLogResponseAdvice} 完成，
 * 脱敏暂不实施（日志仅供运维/开发者内部查看）。
 * <p>
 * <b>日志格式示例：</b>
 * <pre>{@code
 * POST /api/user | params: pageNumber=2, pageSize=15
 * GET /api/user/1 | 200 OK (35ms) | {"code":200,"data":{...}}
 * POST /api/user | EXCEPTION | NullPointerException: Cannot invoke "String.length()"
 * }</pre>
 *
 * @see LabzenLogger
 * @see ApiEndpointLogConfig
 */
public class ApiLogMessageBuilder {

  /**
   * JSON 序列化器
   */
  @Resource
  private ObjectMapper objectMapper;

  /**
   * 打印请求日志。
   * <p>
   * 决策链中确定需要打印请求日志时调用此方法。自动从请求中提取参数，
   * 参数经过过滤和文件类型处理后以 JSON 格式输出。
   *
   * @param controllerClass 生成的 Controller 实现类（用作 Logger 名称）
   * @param config          生效的日志配置
   * @param request         HTTP 请求
   */
  public void logRequest(String controllerClass, ApiEndpointLogConfig config, HttpServletRequest request) {
    LabzenLogger logger = getLogger(controllerClass);
    Level level = config.getLevel();

    if (shouldNotLogging(logger, level)) {
      return;
    }

    String method = request.getMethod();
    String uri = request.getRequestURI();

    //StringBuilder message = new StringBuilder();
    //message.append(method).append(" ").append(uri);

    // 根据配置打印请求参数
    //    if (config.isLogRequestParams()) {
    Map<String, Object> params = extractRequestParams(request);
    Map<String, Object> filtered = filterParams(params, config);
    String message;
    if (filtered.isEmpty()) {
      message = String.format("%s %s", method, uri);
    } else {
      String formattedParams = formatParams(filtered);
      message = String.format("%s %s | %s", method, uri, formattedParams);
      //message.append(" | ").append(formattedParams);
    }
    //    }

    doRequestLogging(logger, level, message);
  }

  /**
   * 从请求中提取客户端实际发送的所有参数。
   * <p>
   * 覆盖全部 HTTP 参数传递方式：
   * <ul>
   *   <li>query string（?a=1&b=2）→ {@link HttpServletRequest#getParameterMap()}</li>
   *   <li>x-www-form-urlencoded → {@link HttpServletRequest#getParameterMap()}</li>
   *   <li>multipart/form-data → {@link HttpServletRequest#getParts()}（文件字段记录元信息）</li>
   *   <li>raw JSON → 读取 body 流（仅当 Content-Type 为 application/json）</li>
   * </ul>
   */
  private Map<String, Object> extractRequestParams(HttpServletRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();

    // 1. query string / x-www-form-urlencoded
    Map<String, String[]> parameterMap = request.getParameterMap();
    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
      String[] values = entry.getValue();
      params.put(entry.getKey(), values.length == 1 ? values[0] : Arrays.asList(values));
    }

    String contentType = request.getContentType();

    // 2. multipart/form-data：文件字段记录元信息，普通表单字段已由 getParameterMap 获取
    boolean isMultipart = contentType != null && contentType.contains("multipart/form-data");
    if (isMultipart) {
      try {
        for (Part part : request.getParts()) {
          String submittedFileName = part.getSubmittedFileName();
          if (submittedFileName != null) {
            MultipartDetail detail = new MultipartDetail(submittedFileName, part.getSize(), part.getContentType());
            //Map<String, Object> fileMeta = new LinkedHashMap<>();
            //fileMeta.put("type", "file");
            //fileMeta.put("fileName", submittedFileName);
            //fileMeta.put("fileSize", part.getSize());
            //fileMeta.put("contentType", part.getContentType());
            params.put(part.getName(), detail);
          }
        }
      } catch (IOException | ServletException e) {
        params.put("_multipart_error", "无法解析 multipart 请求: " + e.getMessage());
      }
    }

    // 3. raw JSON body
    boolean isJson = contentType != null && contentType.contains("application/json");
    if (isJson && params.isEmpty()) {
      try {
        String body = readBody(request);
        if (!body.isBlank()) {
          params.put("_body", body.length() > 4096 ? body.substring(0, 4096) + "...(truncated)" : body);
        }
      } catch (IOException e) {
        params.put("_body_error", "无法读取请求体: " + e.getMessage());
      }
    }

    return params;
  }

  private String readBody(HttpServletRequest request) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    return sb.toString();
  }

  /**
   * 打印响应日志。
   * <p>
   * 在 {@code postHandle} 阶段调用，打印响应状态码、耗时和响应体内容。
   * 响应体由 {@link ApiLogResponseAdvice} 在 {@code beforeBodyWrite} 阶段捕获并存入 request attribute。
   *
   * @param controllerClass Controller 接口名
   * @param config          生效的日志配置
   * @param responseBody    响应体对象（可能为 null，如页面渲染）
   */
  public void logResponse(String controllerClass,
                          ApiEndpointLogConfig config,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          Object responseBody) {
    LabzenLogger logger = getLogger(controllerClass);
    Level level = config.getLevel();

    if (shouldNotLogging(logger, level)) {
      return;
    }

    long executionTime = ControllerDisposeHelper.calculateExecutionTime(request);
    String method = request.getMethod();
    String uri = request.getRequestURI();
    int statusCode = response.getStatus();
    String statusReason;
    try {
      HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
      statusReason = httpStatus.getReasonPhrase();
    } catch (IllegalArgumentException e) {
      HttpStatusExt httpStatusExt = HttpStatusExt.valueOf(statusCode);
      statusReason = httpStatusExt != null ? httpStatusExt.description() : "unknown";
    }

    String message = String.format("%s %s | %d %s (%dms)", method, uri, statusCode, statusReason, executionTime);
    //StringBuilder message = new StringBuilder();
    //message.append(method)
    //       .append(" ")
    //       .append(uri)
    //       .append(" | ")
    //       .append(statusCode)
    //       .append(" ")
    //       .append(statusReason)
    //       .append(" (")
    //       .append(executionTime)
    //       .append("ms)");

    String json = null;
    if (responseBody instanceof Response resp) {
      try {
        json = objectMapper.writeValueAsString(resp);
      } catch (JsonProcessingException e) {
        // ignore that
      }
      //String bodyStr = serializeResponseBody(responseBody);
      //message.append(" | ").append(bodyStr);
    } else if (responseBody != null) {
      message += " | " + responseBody;
    }

    Status status = statusCode >= 400 ? Status.WRONG : Status.SUCCESS;
    doResponseLogging(logger, level, message, status, json);
  }

  /**
   * 打印异常日志。
   * <p>
   * 在 {@code HandlerExceptionResolver} 或 {@code ExceptionCatchingFilter} 中调用，
   * 打印异常类型和消息。
   *
   * @param ex 异常对象
   */
  public void logException(HttpServletRequest request, Exception ex) {
    Object attr = request.getAttribute(API_CONTROLLER_META_ATTRIBUTE);
    String controllerName = "<unknown>";
    LabzenLogger logger;
    if (attr instanceof String in) {
      controllerName = in;
      logger = Loggers.getLogger(in);
    } else {
      logger = Loggers.getLogger(ex.getStackTrace()[0].getClassName());
    }
    logger.atError()
          .status(Status.WRONG)
          .setCause(ex)
          .log("Exception caught with URI: {} | by class {}", request.getRequestURI(), controllerName);

    //    LabzenLogger logger = getLogger(controllerImplClass);
    //    Level level = config.getLevel();
    //
    //    if (shouldNotLogging(logger, level)) {
    //      return;
    //    }
    //
    //    String implClassName = controllerImplClass != null
    //      ? controllerImplClass.getSimpleName()
    //      : "UnknownController";
    //
    //    String exceptionType = exception.getClass().getSimpleName();
    //    String exceptionMessage = exception.getMessage() != null ? exception.getMessage() : "(no message)";
    //
    //    String message = implClassName + " | EXCEPTION | " + exceptionType + ": " + exceptionMessage;
    //
    //    logAtLevel(logger, level, message, "BAD-REQUEST");
  }

  /**
   * 获取 LabzenLogger 实例。
   * <p>
   * Logger 名称使用 Controller 实现类的全限定名，确保日志输出中明确标识类名。
   *
   * @param clazz Controller 实现类
   * @return LabzenLogger 实例
   */
  //private LabzenLogger getLogger(Class<?> clazz) {
  //  Logger slf4jLogger = Loggers.getLogger(clazz);
  //  if (slf4jLogger instanceof LabzenLogger labzenLogger) {
  //    return labzenLogger;
  //  }
  //  // 降级：如果 Logger 不是 LabzenLogger 实例，使用普通 SLF4J
  //  throw new IllegalStateException("SLF4J Logger is not a LabzenLogger instance");
  //}
  private LabzenLogger getLogger(String clazz) {
    Logger slf4jLogger = Loggers.getLogger(clazz);
    if (slf4jLogger instanceof LabzenLogger labzenLogger) {
      return labzenLogger;
    }
    // 降级：如果 Logger 不是 LabzenLogger 实例，使用普通 SLF4J
    throw new IllegalStateException("SLF4J Logger is not a LabzenLogger instance");
  }

  /**
   * 判断是否应该输出日志（级别检查）。
   */
  private boolean shouldNotLogging(LabzenLogger logger, Level level) {
    return !switch (level) {
      case TRACE -> logger.isTraceEnabled();
      case DEBUG -> logger.isDebugEnabled();
      case INFO -> logger.isInfoEnabled();
      case WARN -> logger.isWarnEnabled();
      case ERROR -> true; // ERROR 总是输出
    };
  }

  /**
   * 按指定级别输出日志。
   *
   * @param logger  LabzenLogger 实例
   * @param level   日志级别
   * @param message 日志消息
   */
  private void doRequestLogging(LabzenLogger logger, Level level, String message) {
    switch (level) {
      case TRACE -> logger.atTrace().scene(Scenes.REQUEST).log(message);
      case DEBUG -> logger.atDebug().scene(Scenes.REQUEST).log(message);
      case INFO -> logger.atInfo().scene(Scenes.REQUEST).log(message);
      case WARN -> logger.atWarn().scene(Scenes.REQUEST).log(message);
      case ERROR -> logger.atError().scene(Scenes.REQUEST).log(message);
    }
  }

  /**
   * 按指定级别输出日志。
   *
   * @param logger  LabzenLogger 实例
   * @param level   日志级别
   * @param message 日志消息
   * @param status  日志状态
   * @param content 响应内容
   */
  private void doResponseLogging(LabzenLogger logger, Level level, String message, Status status, String content) {
    //  logAtLevel(logger, level, message, scenes.name(), status.getText());
    //}
    //
    ///**
    // * 按指定级别输出日志。
    // *
    // * @param logger  LabzenLogger 实例
    // * @param level   日志级别
    // * @param message 日志消息
    // * @param scenes  日志场景
    // * @param status  日志状态
    // */
    //private void logAtLevel(LabzenLogger logger, Level level, String message, String scenes, String status) {
    var builder = switch (level) {
      case TRACE -> logger.atTrace();
      case DEBUG -> logger.atDebug();
      case INFO -> logger.atInfo();
      case WARN -> logger.atWarn();
      case ERROR -> logger.atError();
    };
    builder.scene(Scenes.RESPONSE).status(status);
    if (Strings.isNotBlank(content)) {
      builder.json(content);
    }
    builder.log(message);
  }

  /**
   * 过滤请求参数。
   * <p>
   * 规则：
   * <ul>
   *   <li>若 {@code includeParams} 非空，仅保留白名单中的参数（大小写不敏感）</li>
   *   <li>否则应用 {@code excludeParams} 黑名单，排除敏感参数</li>
   *   <li>文件类型参数（MultipartFile/Part）替换为元信息 Map</li>
   * </ul>
   *
   * @param params 原始参数 Map
   * @param config 日志配置
   * @return 过滤后的参数 Map
   */
  public Map<String, Object> filterParams(Map<String, Object> params, ApiEndpointLogConfig config) {
    if (params == null || params.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> includeParams = config.getIncludeParams();
    Set<String> excludeParams = config.getExcludeParams();

    Map<String, Object> filtered = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      // 白名单优先
      if (includeParams != null && !includeParams.isEmpty()) {
        if (!containsIgnoreCase(includeParams, key)) {
          continue;
        }
      } else {
        // 黑名单过滤
        if (containsIgnoreCase(excludeParams, key)) {
          filtered.put(key, "***");
          continue;
        }
      }

      // 文件类型参数特殊处理：打印元信息而非二进制内容
      filtered.put(key, serializeParamValue(value));
    }

    return filtered;
  }

  /**
   * 将参数 Map 格式化为可读字符串。
   * <p>
   * 使用 {@code key=value} 格式，多参数用逗号分隔，不进行 JSON 序列化以规避转义问题。
   * Map 类型的值（如文件元信息）递归格式化。
   */
  private String formatParams(Map<String, Object> params) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append("=");
      Object value = entry.getValue();
      if (value instanceof Map<?, ?> map) {
        sb.append("{");
        int j = 0;
        for (Map.Entry<?, ?> me : map.entrySet()) {
          if (j > 0) {
            sb.append(", ");
          }
          sb.append(me.getKey()).append("=").append(me.getValue());
          j++;
        }
        sb.append("}");
      } else if (value instanceof List<?> list) {
        sb.append(list);
      } else {
        sb.append(value);
      }
      i++;
    }
    return sb.toString();
  }

  /**
   * 序列化单个参数值。
   * <p>
   * 文件类型返回元信息 Map，普通类型直接返回。
   */
  private Object serializeParamValue(Object value) {
    switch (value) {
      case null -> {
        return null;
      }

      // 处理 MultipartFile
      case MultipartFile file -> {
        return buildFileMeta(file.getOriginalFilename(), file.getSize(), file.getContentType());
      }

      // 处理 Jakarta Servlet Part
      case Part part -> {
        return buildFileMeta(part.getSubmittedFileName(), part.getSize(), part.getContentType());
      }
      default -> {
      }
    }

    // 处理 Spring MockMultipartFile 等（反射检查）
    String className = value.getClass().getName();
    if (className.contains("MultipartFile")) {
      try {
        String name = (String) value.getClass().getMethod("getOriginalFilename").invoke(value);
        long size = (long) value.getClass().getMethod("getSize").invoke(value);
        String contentType = (String) value.getClass().getMethod("getContentType").invoke(value);
        return buildFileMeta(name, size, contentType);
      } catch (Exception ignored) {
        return "[Binary File]";
      }
    }

    // 数组类型转为列表
    if (value.getClass().isArray()) {
      return Arrays.toString((Object[]) value);
    }

    return value;
  }

  /**
   * 构建文件元信息 Map。
   */
  private Map<String, Object> buildFileMeta(String fileName, long fileSize, String contentType) {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("type", "file");
    meta.put("fileName", fileName);
    meta.put("fileSize", fileSize);
    meta.put("contentType", contentType);
    return meta;
  }

  /**
   * 安全地将对象序列化为 JSON 字符串。
   * <p>
   * 序列化失败时返回对象的 toString() 结果。
   */
  //private String toJsonSafely(Object obj) {
  //  if (obj == null) {
  //    return "null";
  //  }
  //  if (obj instanceof String s) {
  //    return s;
  //  }
  //  try {
  //    return objectMapper.writeValueAsString(obj);
  //  } catch (JsonProcessingException e) {
  //    return obj.toString();
  //  }
  //}

  // ============================================================
  // 响应体处理
  // ============================================================

  private static final int MAX_BODY_LENGTH = 4096;

  /**
   * 处理响应体为日志可用的字符串。
   * <p>
   * 处理逻辑：
   * <ul>
   *   <li>文件下载（{@link FileResult}）：打印文件元信息而非二进制内容</li>
   *   <li>已为字符串：直接截断</li>
   *   <li>其他对象：JSON 序列化后截断</li>
   * </ul>
   */
  private String serializeResponseBody(Object body) {
    if (body == null) {
      return "null";
    }

    // 文件下载响应
    if (body instanceof FileResult fileResult) {
      File file = fileResult.value();
      return "{type:file, filename:" + fileResult.filename() + ", size:" + file.length() + "}";
    }

    // 已为字符串（如 LabzenRestResponseBodyAdvice 已转为 JSON 字符串）
    if (body instanceof String str) {
      return truncate(str);
    }

    // 其他对象：JSON 序列化
    try {
      return truncate(objectMapper.writeValueAsString(body));
    } catch (JsonProcessingException e) {
      return "[无法序列化响应体: " + e.getMessage() + "]";
    }
  }

  private String truncate(String value) {
    if (value == null) {
      return null;
    }
    if (value.length() > MAX_BODY_LENGTH) {
      return value.substring(0, MAX_BODY_LENGTH) + "...(truncated, total " + value.length() + " chars)";
    }
    return value;
  }

  /**
   * 大小写不敏感地检查集合中是否包含指定字符串。
   */
  private boolean containsIgnoreCase(Set<String> set, String value) {
    if (set == null || value == null) {
      return false;
    }
    return set.stream().anyMatch(s -> s.equalsIgnoreCase(value));
  }
}
