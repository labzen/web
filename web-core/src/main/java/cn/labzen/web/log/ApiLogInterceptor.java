package cn.labzen.web.log;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.meta.WebCoreConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static cn.labzen.web.api.definition.Constants.API_LOG_CONFIG_ATTRIBUTE;
import static cn.labzen.web.api.definition.Constants.API_LOG_CONTROLLER_META_ATTRIBUTE;

/**
 * API 日志请求拦截器。
 * <p>
 * 在 {@code preHandle} 阶段执行完整的日志决策链：
 * <ol>
 *   <li>通过 {@link LoggableControllerMetaRegistry} 查找 Controller 元数据（O(1) 查表）</li>
 *   <li>解析端点配置（三层合并，包含可能的 conditionGroup）</li>
 *   <li>若有条件配置 → 评估条件，不匹配则跳过</li>
 *   <li>采样率检查</li>
 *   <li>打印请求日志</li>
 * </ol>
 *
 * @see ApiLogConfigManager
 * @see LoggableControllerMetaRegistry
 * @see ApiLogConditionEvaluator
 * @see ApiLogMessageBuilder
 * @see ApiLogResponseAdvice
 */
public class ApiLogInterceptor implements HandlerInterceptor {

  private final LoggableControllerMetaRegistry controllerRegistry;
  private final ApiLogConfigManager configManager;
  private final ApiLogConditionEvaluator conditionEvaluator = new ApiLogConditionEvaluator();
  private final ApiLogMessageBuilder messageBuilder = new ApiLogMessageBuilder();
  private final WebCoreConfiguration configuration;

  /**
   * 方法签名哈希缓存，避免每次请求都反射计算
   */
  private final Map<java.lang.reflect.Method, String> methodHashCache = new ConcurrentHashMap<>();

  public ApiLogInterceptor(LoggableControllerMetaRegistry controllerRegistry) {
    this.controllerRegistry = controllerRegistry;
    this.configManager = new ApiLogConfigManager(controllerRegistry);
    this.configuration = Labzens.configurationWith(WebCoreConfiguration.class);
  }

  @Override
  public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
    if (!configuration.apiLogEnabled()) {
      return true;
    }
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    try {
      Class<?> controllerClass = handlerMethod.getBeanType();
      String controllerSimpleName = controllerClass.getSimpleName();

      // 步骤1：查找 Controller 元数据
      Optional<ControllerMeta> metaOpt = controllerRegistry.lookup(controllerSimpleName);
      if (metaOpt.isEmpty()) {
        return true;
      }
      ControllerMeta controllerMeta = metaOpt.get();
      request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);

      // 步骤2：通过方法签名哈希获取端点配置
      String methodHash = computeMethodHash(handlerMethod);
      ApiEndpointLogConfig config = configManager.resolveConfig(controllerSimpleName, methodHash);
      if (config == null || !config.isEnabled()) {
        return true;
      }

      // 步骤3：采样率检查
      if (!checkSampling(config.getSamplingRate())) {
        return true;
      }

      // 步骤4：若有条件配置，评估条件
      if (config.isConditional() && !conditionEvaluator.evaluate(config, request)) {
        return true;
      }

      // 步骤5：打印请求日志
      logRequest(controllerClass, config, request);
      request.setAttribute(API_LOG_CONFIG_ATTRIBUTE, config);
    } catch (Exception e) {
      // 日志拦截器的任何异常都不应影响正常业务流程
    }

    return true;
  }

  // ============================================================
  // 方法签名哈希（带缓存）
  // ============================================================

  /**
   * 计算 HandlerMethod 的方法签名哈希。
   * <p>
   * 签名格式与 APT 编译期 MetadataGenerateProcessor 保持一致：
   * {@code methodName:returnType(paramType1,paramType2,...)} → MD5 前 5 位 hex。
   * <p>
   * 使用 {@link ConcurrentHashMap} 缓存，同一方法只计算一次。
   */

  private String computeMethodHash(HandlerMethod handlerMethod) {
    java.lang.reflect.Method method = handlerMethod.getMethod();
    return methodHashCache.computeIfAbsent(method, m -> {
      String methodName = m.getName();
      String returnType = m.getReturnType().getSimpleName();
      List<String> paramTypes = Arrays.stream(m.getParameterTypes())
        .map(Class::getSimpleName)
        .toList();
      return LogUtils.hashControllerMethod(methodName, returnType, paramTypes);
    });
  }

  // ============================================================
  // 日志打印
  // ============================================================

  private void logRequest(Class<?> controllerClass, ApiEndpointLogConfig config, HttpServletRequest request) {
    Map<String, Object> params = extractRequestParams(request);
    messageBuilder.logRequest(controllerClass, config, request, params);
  }

  private Map<String, Object> extractRequestParams(HttpServletRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();
    Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String name = paramNames.nextElement();
      String[] values = request.getParameterValues(name);
      if (values != null) {
        params.put(name, values.length == 1 ? values[0] : Arrays.asList(values));
      }
    }
    return params;
  }

  // ============================================================
  // 采样
  // ============================================================

  private boolean checkSampling(double samplingRate) {
    if (samplingRate >= 1.0) return true;
    if (samplingRate <= 0.0) return false;
    return ThreadLocalRandom.current().nextDouble() < samplingRate;
  }
}
