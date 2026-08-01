package cn.labzen.web.log;

import cn.labzen.web.api.log.ApiLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.api.log.registry.LoggableControllerMetaRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static cn.labzen.web.api.definition.Constants.API_LOG_CONFIG_ATTRIBUTE;
import static cn.labzen.web.api.definition.Constants.API_LOG_CONTROLLER_META_ATTRIBUTE;

/**
 * API 日志请求拦截器。
 * <p>
 * 在 {@code preHandle} 阶段执行完整的日志决策链，决定是否打印当前请求的日志。
 * 核心流程：
 * <ol>
 *   <li>通过 {@link LoggableControllerMetaRegistry} 查找 Controller 元数据（O(1) 查表）</li>
 *   <li>解析最终生效的配置（三层合并）</li>
 *   <li>若配置为条件触发模式（{@code matchType != null}），通过 {@link ApiLogConditionEvaluator} 匹配请求参数</li>
 *   <li>采样率检查</li>
 *   <li>打印请求日志，传递配置到响应阶段</li>
 * </ol>
 * <p>
 * <b>请求属性传递：</b>将生效的配置和元数据存入 {@code request.setAttribute()}，
 * 供 {@link ApiLogResponseAdvice} 在响应阶段使用，避免重复计算。
 * <p>
 * <b>线程安全：</b>无状态设计，所有状态通过 {@link ApiLogConfigManager} 管理。
 *
 * @see ApiLogConfigManager
 * @see LoggableControllerMetaRegistry
 * @see ApiLogConditionEvaluator
 * @see ApiLogMessageBuilder
 * @see ApiLogResponseAdvice
 */
public class ApiLogInterceptor implements HandlerInterceptor {

  private final ApiLogConfigManager configManager;
  private final LoggableControllerMetaRegistry controllerRegistry;
  private final ApiLogConditionEvaluator conditionEvaluator;
  private final ApiLogMessageBuilder messageBuilder;

  /**
   * 构造函数，注入依赖组件。
   */
  public ApiLogInterceptor(
    ApiLogConfigManager configManager,
    LoggableControllerMetaRegistry controllerRegistry,
    ApiLogConditionEvaluator conditionEvaluator,
    ApiLogMessageBuilder messageBuilder
  ) {
    this.configManager = configManager;
    this.controllerRegistry = controllerRegistry;
    this.conditionEvaluator = conditionEvaluator;
    this.messageBuilder = messageBuilder;
  }

  /**
   * 前置处理：执行完整的日志决策链。
   * <p>
   * 只处理 Spring MVC 的 HandlerMethod（Controller 方法），
   * 对静态资源等其他 handler 类型直接放行。
   */
  @Override
  public boolean preHandle(
    @Nonnull HttpServletRequest request,
    @Nonnull HttpServletResponse response,
    @Nonnull Object handler
  ) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    try {
      Class<?> controllerClass = handlerMethod.getBeanType();
      String controllerSimpleName = controllerClass.getSimpleName();

      // 步骤1：从注册表查找 Controller 元数据（O(1) 查表）
      Optional<ControllerMeta> metaOpt =
        controllerRegistry.lookup(controllerSimpleName);

      if (metaOpt.isEmpty()) {
        return true;
      }

      ControllerMeta controllerMeta = metaOpt.get();
      String methodName = handlerMethod.getMethod().getName();
      String httpMethod = request.getMethod();
      String requestUri = request.getRequestURI();

      // 尝试多种 methodKey：方法名、HTTP方法+URL
      String methodKey1 = methodName;
      String methodKey2 = httpMethod + " " + requestUri;

      // 步骤2：解析最终生效的配置（三层合并）
      ApiLogConfig config = resolveEffectiveConfig(controllerSimpleName, methodKey1, methodKey2);

      // 步骤2.1：额外检查活跃的条件配置（来自 registerCondition）
      ApiLogConfig conditionalConfig = tryMatchConditionalConfig(
        controllerSimpleName, methodKey1, methodKey2, request);
      if (conditionalConfig != null) {
        // 条件匹配成功
        logRequest(controllerClass, controllerMeta, conditionalConfig, request);
        request.setAttribute(API_LOG_CONFIG_ATTRIBUTE, conditionalConfig);
        request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);
        return true;
      }

      // 步骤3：检查是否启用
      if (!config.isEnabled()) {
        request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);
        return true;
      }

      // 步骤4：若为条件触发模式，评估条件
      if (config.isConditional()) {
        if (!conditionEvaluator.evaluate(config, request)) {
          // 条件不匹配，跳过
          request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);
          return true;
        }
      }

      // 步骤5：采样率检查
      if (!checkSampling(config.getSamplingRate())) {
        request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);
        return true;
      }

      // 步骤6：打印请求日志
      logRequest(controllerClass, controllerMeta, config, request);
      request.setAttribute(API_LOG_CONFIG_ATTRIBUTE, config);
      request.setAttribute(API_LOG_CONTROLLER_META_ATTRIBUTE, controllerMeta);

    } catch (Exception e) {
      // 日志拦截器的任何异常都不应影响正常业务流程
    }

    return true;
  }

  /**
   * 尝试匹配活跃的条件配置（来自 registerCondition）。
   */
  private ApiLogConfig tryMatchConditionalConfig(
    String controllerSimpleName,
    String methodKey1,
    String methodKey2,
    HttpServletRequest request
  ) {
    List<ApiLogConfig> conditions = configManager.getActiveConditionalConfigs();
    if (conditions.isEmpty()) {
      return null;
    }

    for (ApiLogConfig condition : conditions) {
      if (condition.isExpired()) {
        continue;
      }
      if (conditionEvaluator.evaluate(condition, request)) {
        return condition;
      }
    }

    return null;
  }

  /**
   * 解析最终生效的配置（三层合并）。
   */
  private ApiLogConfig resolveEffectiveConfig(
    String controllerSimpleName,
    String methodKey1,
    String methodKey2
  ) {
    ApiLogConfig config = configManager.resolveConfig(controllerSimpleName, methodKey1);
    // 如果按方法名没找到特定配置，尝试 HTTP方法+URL
    ApiLogConfig urlConfig = configManager.resolveConfig(controllerSimpleName, methodKey2);
    // 合并两个查找结果（urlConfig 的字段会覆盖 methodKey1 的结果）
    if (urlConfig != null && urlConfig != ApiLogConfig.frameDefaults()) {
      return config.mergeFrom(urlConfig);
    }
    return config;
  }

  /**
   * 打印请求日志。
   */
  private void logRequest(
    Class<?> controllerClass,
    ControllerMeta controllerMeta,
    ApiLogConfig config,
    HttpServletRequest request
  ) {
    Map<String, Object> params = extractRequestParams(request);
    messageBuilder.logRequest(controllerClass, controllerMeta, config, request, params);
  }

  /**
   * 从请求中提取所有参数（Query 参数）。
   */
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

  /**
   * 采样检查：按采样率决定是否打印本次请求日志。
   */
  private boolean checkSampling(double samplingRate) {
    if (samplingRate >= 1.0) {
      return true;
    }
    if (samplingRate <= 0.0) {
      return false;
    }
    return ThreadLocalRandom.current().nextDouble() < samplingRate;
  }
}
