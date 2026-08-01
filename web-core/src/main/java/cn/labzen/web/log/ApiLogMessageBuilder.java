package cn.labzen.web.log;

import cn.labzen.logger.Loggers;
import cn.labzen.logger.kernel.LabzenLogger;
import cn.labzen.logger.kernel.enums.Status;
import cn.labzen.web.api.log.ApiLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.response.result.FileResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static cn.labzen.web.api.definition.Constants.LOGGER_SCENE_API_LOG;

/**
 * API 日志消息构建工具。
 * <p>
 * 负责构建结构化 API 日志消息，使用 {@link LabzenLogger} 的结构化 API 输出日志。
 * 核心功能：
 * <ul>
 *   <li><b>参数过滤</b>：按配置的 {@code includeParams}（白名单）或 {@code excludeParams}（黑名单）过滤请求参数</li>
 *   <li><b>文件处理</b>：识别上传文件类型（{@link MultipartFile}/{@link Part}）打印元信息而非二进制内容</li>
 *   <li><b>响应体脱敏</b>：按配置的 {@code responseMaskPatterns} 对 JSON 响应体执行正则脱敏</li>
 *   <li><b>响应体截断</b>：限制最大 4096 字符防止日志膨胀</li>
 *   <li><b>调用栈定位</b>：通过 {@link Thread#getStackTrace()} 计算 Logger 调用者的类名、方法名和行号</li>
 * </ul>
 * <p>
 * <b>日志格式示例：</b>
 * <pre>{@code
 * [API-LOG] UserControllerImpl#create | REQUEST  | POST /api/user | params: {"name":"张三","email":"test@example.com"}
 * [API-LOG] UserControllerImpl#create | RESPONSE | status=200 | body: {"code":200,"message":"success"} | cost=15ms
 * [API-LOG] UserControllerImpl#create | EXCEPTION | NullPointerException: Cannot invoke "String.length()"
 * }</pre>
 * <p>
 * <b>预置脱敏规则：</b>
 * <ul>
 *   <li>{@code phone-mask}：手机号中间四位替换为星号（138****5678）</li>
 *   <li>{@code idcard-mask}：身份证号中间八位替换为星号（3201**********1234）</li>
 * </ul>
 *
 * @see LabzenLogger
 * @see ApiLogConfig
 */
public class ApiLogMessageBuilder {

  /**
   * 响应体最大字符数限制
   */
  private static final int MAX_BODY_LENGTH = 4096;

  /**
   * 手机号脱敏正则：匹配中间四位
   */
  private static final Pattern PHONE_MASK_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

  /**
   * 手机号脱敏替换
   */
  private static final String PHONE_MASK_REPLACEMENT = "$1****$2";

  /**
   * 身份证号脱敏正则：匹配中间八位
   */
  private static final Pattern IDCARD_MASK_PATTERN = Pattern.compile("(\\d{4})\\d{8}(\\d{4})");

  /**
   * 身份证号脱敏替换
   */
  private static final String IDCARD_MASK_REPLACEMENT = "$1********$2";

  /**
   * JSON 序列化器
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * 打印请求日志。
   * <p>
   * 决策链中确定需要打印请求日志时调用此方法。根据配置决定是否打印请求参数，
   * 参数经过过滤和文件类型处理后以 JSON 格式输出。
   *
   * @param controllerImplClass 生成的 Controller 实现类（用作 Logger 名称）
   * @param controllerMeta      元数据（接口名、方法信息）
   * @param config              生效的日志配置
   * @param request             HTTP 请求
   * @param params              请求参数 Map（key=参数名, value=参数值）
   */
  public void logRequest(
    Class<?> controllerImplClass,
    ControllerMeta controllerMeta,
    ApiLogConfig config,
    HttpServletRequest request,
    Map<String, Object> params
  ) {
    LabzenLogger logger = getLogger(controllerImplClass);
    Level level = config.getLevel();

    if (shouldNotLogging(logger, level)) {
      return;
    }

    String implClassName = controllerImplClass.getSimpleName();
    String method = request.getMethod();
    String uri = request.getRequestURI();

    StringBuilder message = new StringBuilder();
    message.append(implClassName).append(" | REQUEST | ")
      .append(method).append(" ").append(uri);

    // 根据配置打印请求参数
    if (config.isLogRequestParams()) {
      Map<String, Object> filtered = filterParams(params, config);
      String paramsJson = toJsonSafely(filtered);
      message.append(" | params: ").append(paramsJson);
    }

    logAtLevel(logger, level, message.toString(), Status.NORMAL);
  }

  /**
   * 打印响应日志。
   * <p>
   * 在 {@code beforeBodyWrite} 中调用，打印响应状态码和响应体内容。
   * 响应体经过脱敏和截断处理。
   *
   * @param controllerImplClass Controller 实现类
   * @param controllerMeta      元数据
   * @param config              生效的日志配置
   * @param statusCode          HTTP 状态码
   * @param body                响应体对象
   * @param costMs              请求处理耗时（毫秒）
   */
  public void logResponse(
    Class<?> controllerImplClass,
    ControllerMeta controllerMeta,
    ApiLogConfig config,
    int statusCode,
    Object body,
    long costMs
  ) {
    LabzenLogger logger = getLogger(controllerImplClass);
    String level = config.getLevel();

    if (shouldNotLogging(logger, level)) {
      return;
    }

    String implClassName = controllerImplClass.getSimpleName();

    StringBuilder message = new StringBuilder();
    message.append(implClassName).append(" | RESPONSE | ")
      .append("status=").append(statusCode);

    // 根据配置打印响应体
    if (config.isLogResponseBody()) {
      String bodyStr = serializeResponseBody(body, config);
      message.append(" | body: ").append(bodyStr);
    }

    message.append(" | cost=").append(costMs).append("ms");

    Status status = statusCode >= 400 ? Status.WARNING : Status.NORMAL;
    logAtLevel(logger, level, message.toString(), status);
  }

  /**
   * 打印异常日志。
   * <p>
   * 在 {@code HandlerExceptionResolver} 或 {@code ExceptionCatchingFilter} 中调用，
   * 打印异常类型和消息。
   *
   * @param controllerImplClass Controller 实现类
   * @param controllerMeta      元数据（可能为 null，过滤器阶段可能获取不到）
   * @param config              生效的日志配置
   * @param exception           异常对象
   */
  public void logException(
    Class<?> controllerImplClass,
    ControllerMeta controllerMeta,
    ApiLogConfig config,
    Exception exception
  ) {
    if (config == null || !config.isLogException()) {
      return;
    }

    LabzenLogger logger = getLogger(controllerImplClass);
    String level = config.getLevel();

    if (shouldNotLogging(logger, level)) {
      return;
    }

    String implClassName = controllerImplClass != null
      ? controllerImplClass.getSimpleName()
      : "UnknownController";

    String exceptionType = exception.getClass().getSimpleName();
    String exceptionMessage = exception.getMessage() != null ? exception.getMessage() : "(no message)";

    String message = implClassName + " | EXCEPTION | " + exceptionType + ": " + exceptionMessage;

    logAtLevel(logger, level, message, Status.ERROR);
  }

  /**
   * 获取 LabzenLogger 实例。
   * <p>
   * Logger 名称使用 Controller 实现类的全限定名，确保日志输出中明确标识类名。
   *
   * @param clazz Controller 实现类
   * @return LabzenLogger 实例
   */
  private LabzenLogger getLogger(Class<?> clazz) {
    Logger slf4jLogger = Loggers.getLogger(clazz);
    if (slf4jLogger instanceof LabzenLogger labzenLogger) {
      return labzenLogger;
    }
    // 降级：如果 Logger 不是 LabzenLogger 实例，使用普通 SLF4J
    return new LabzenLoggerAdapter(slf4jLogger);
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
   * @param status  状态标记
   */
  private void logAtLevel(LabzenLogger logger, String level, String message, Status status) {
    switch (level.toUpperCase()) {
      case "TRACE" -> logger.atTrace().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
      case "DEBUG" -> logger.atDebug().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
      case "INFO" -> logger.atInfo().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
      case "WARN" -> logger.atWarn().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
      case "ERROR" -> logger.atError().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
      default -> logger.atDebug().scene(LOGGER_SCENE_API_LOG).status(status).log(message);
    }
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
  public Map<String, Object> filterParams(Map<String, Object> params, ApiLogConfig config) {
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
   * 序列化单个参数值。
   * <p>
   * 文件类型返回元信息 Map，普通类型直接返回。
   */
  private Object serializeParamValue(Object value) {
    if (value == null) {
      return null;
    }

    // 处理 MultipartFile
    if (value instanceof MultipartFile file) {
      return buildFileMeta(file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    // 处理 Jakarta Servlet Part
    if (value instanceof Part part) {
      return buildFileMeta(part.getSubmittedFileName(), part.getSize(), part.getContentType());
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
   * 序列化响应体。
   * <p>
   * 处理逻辑：
   * <ul>
   *   <li>文件下载类型（{@link FileResult}）：打印文件元信息而非二进制内容</li>
   *   <li>普通对象：序列化为 JSON 后执行脱敏和截断</li>
   * </ul>
   *
   * @param body   响应体对象
   * @param config 日志配置
   * @return 序列化后的字符串
   */
  private String serializeResponseBody(Object body, ApiLogConfig config) {
    if (body == null) {
      return "null";
    }

    // 文件下载响应：打印元信息
    if (body instanceof FileResult fileResult) {
      File file = fileResult.value();
      return "{type:file, filename:" + fileResult.filename() + ", size:" + file.length() + "}";
    }

    // 普通对象：JSON 序列化
    String json = toJsonSafely(body);

    // 脱敏处理
    json = applyMasking(json, config.getResponseMaskPatterns());

    // 截断处理
    if (json.length() > MAX_BODY_LENGTH) {
      json = json.substring(0, MAX_BODY_LENGTH) + "...(truncated, total " + json.length() + " chars)";
    }

    return json;
  }

  /**
   * 对 JSON 字符串执行脱敏处理。
   * <p>
   * 按配置的 JSON Path 模式匹配字段名，对匹配到的字段值应用脱敏规则。
   * <p>
   * 实现策略：使用正则表达式在 JSON 字符串中查找字段并替换。
   * 简化实现——按 key 查找 JSON 字段，对匹配到的值应用脱敏。
   *
   * @param json     JSON 字符串
   * @param patterns 脱敏规则 Map（JSON Path → 规则名）
   * @return 脱敏后的 JSON 字符串
   */
  String applyMasking(String json, Map<String, String> patterns) {
    if (json == null || json.isEmpty() || patterns == null || patterns.isEmpty()) {
      return json;
    }

    String result = json;
    for (Map.Entry<String, String> entry : patterns.entrySet()) {
      String jsonPath = entry.getKey();
      String ruleName = entry.getValue();

      // 提取 JSON Path 中最后的字段名（如 "$.phone" → "phone"）
      String fieldName = extractFieldName(jsonPath);
      if (fieldName == null) {
        continue;
      }

      // 查找 JSON 中的该字段并应用脱敏规则
      result = maskField(result, fieldName, ruleName);
    }

    return result;
  }

  /**
   * 从 JSON Path 中提取字段名。
   * <p>
   * 示例：{@code "$.phone"} → {@code "phone"}；{@code "$.records[*].phone"} → {@code "phone"}
   */
  private String extractFieldName(String jsonPath) {
    if (jsonPath == null) {
      return null;
    }
    // 去掉 $ 前缀和数组通配符
    String cleaned = jsonPath.replace("$", "").replace("[*]", "");
    // 按 "." 分割取最后一段
    int lastDot = cleaned.lastIndexOf('.');
    return lastDot >= 0 ? cleaned.substring(lastDot + 1) : cleaned;
  }

  /**
   * 对 JSON 中指定字段的值应用脱敏规则。
   */
  private String maskField(String json, String fieldName, String ruleName) {
    // 匹配 JSON 字段模式: "fieldName": "value" 或 "fieldName":"value"
    Pattern pattern = Pattern.compile(
      "\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\""
    );
    java.util.regex.Matcher matcher = pattern.matcher(json);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String originalValue = matcher.group(1);
      String maskedValue = applyMaskRule(originalValue, ruleName);
      matcher.appendReplacement(sb,
        "\"" + fieldName + "\":\"" + Matcher.quoteReplacement(maskedValue) + "\"");
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * 应用脱敏规则到单个字段值。
   *
   * @param value    原始值
   * @param ruleName 规则名称
   * @return 脱敏后的值
   */
  private String applyMaskRule(String value, String ruleName) {
    if (value == null) {
      return null;
    }
    return switch (ruleName) {
      case "phone-mask" -> PHONE_MASK_PATTERN.matcher(value).replaceAll(PHONE_MASK_REPLACEMENT);
      case "idcard-mask" -> IDCARD_MASK_PATTERN.matcher(value).replaceAll(IDCARD_MASK_REPLACEMENT);
      default -> value; // 未知规则，不处理
    };
  }

  /**
   * 安全地将对象序列化为 JSON 字符串。
   * <p>
   * 序列化失败时返回对象的 toString() 结果。
   */
  private String toJsonSafely(Object obj) {
    if (obj == null) {
      return "null";
    }
    if (obj instanceof String s) {
      return s;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return obj.toString();
    }
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

  /**
   * LabzenLogger 适配器。
   * <p>
   * 当 SLF4J Logger 不是 LabzenLogger 实例时（降级场景），
   * 将结构化 API 调用转换为普通的 SLF4J 日志调用。
   */
  private static class LabzenLoggerAdapter implements LabzenLogger {

    private final Logger delegate;

    LabzenLoggerAdapter(Logger delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
      return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
      delegate.trace(msg);
    }

    @Override
    public void trace(String format, Object... arguments) {
      delegate.trace(format, arguments);
    }

    @Override
    public boolean isDebugEnabled() {
      return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
      delegate.debug(msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
      delegate.debug(format, arguments);
    }

    @Override
    public boolean isInfoEnabled() {
      return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
      delegate.info(msg);
    }

    @Override
    public void info(String format, Object... arguments) {
      delegate.info(format, arguments);
    }

    @Override
    public boolean isWarnEnabled() {
      return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
      delegate.warn(msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
      delegate.warn(format, arguments);
    }

    @Override
    public boolean isErrorEnabled() {
      return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
      delegate.error(msg);
    }

    @Override
    public void error(String format, Object... arguments) {
      delegate.error(format, arguments);
    }

    // 降级场景下不支持结构化 API，仅使用普通 SLF4J
    @Override
    public LabzenLogger atTrace() {
      return this;
    }

    @Override
    public LabzenLogger atDebug() {
      return this;
    }

    @Override
    public LabzenLogger atInfo() {
      return this;
    }

    @Override
    public LabzenLogger atWarn() {
      return this;
    }

    @Override
    public LabzenLogger atError() {
      return this;
    }

    @Override
    public LabzenLogger scene(String scene) {
      return this;
    }

    @Override
    public LabzenLogger status(Status status) {
      return this;
    }

    @Override
    public void log(String msg) {
      delegate.info(msg);
    }

    @Override
    public void log(String format, Object... arguments) {
      delegate.info(format, arguments);
    }
  }
}
