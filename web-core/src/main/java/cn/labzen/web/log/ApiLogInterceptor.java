package cn.labzen.web.log;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.registry.ControllerMeta;
import cn.labzen.web.util.ControllerDisposeHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static cn.labzen.web.api.definition.Constants.*;

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
 */
@Slf4j
public class ApiLogInterceptor implements HandlerInterceptor {

  private final ApiLogConditionEvaluator conditionEvaluator = new ApiLogConditionEvaluator();
  //  private final WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
  // todo 需要暴露一个方法，清除缓存
  private final Map<Class<?>, String> controllerInterfaceNameCache = new ConcurrentHashMap<>();

  @Resource
  private ApiLogConfigManager configManager;
  @Resource
  private LoggableControllerMetaRegistry controllerRegistry;
  @Resource
  private ApiLogMessageBuilder messageBuilder;

  /**
   * 方法签名哈希缓存，避免每次请求都反射计算
   */
  private final Map<java.lang.reflect.Method, String> methodHashCache = new ConcurrentHashMap<>();

  //  public ApiLogInterceptor(ApiLogConfigManager configManager,
  //                           LoggableControllerMetaRegistry controllerRegistry,
  //                           ApiLogMessageBuilder messageBuilder) {
  //    this.configManager = configManager;
  //    this.controllerRegistry = controllerRegistry;
  //    this.messageBuilder = messageBuilder;
  //  }

  //  private record Endpoint(ControllerMeta controllerMeta, ApiEndpointLogConfig config) {
  //  }
  //
  //
  //  private Endpoint resolveConfig(HttpServletRequest request, HandlerMethod handlerMethod, Class<?> controllerClass) {
  //    // 步骤1：通过实现类接口查找 Controller 元数据
  //    Optional<ControllerMeta> metaOpt = resolveControllerMeta(controllerClass);
  //    if (metaOpt.isEmpty()) {
  //      return null;
  //    }
  //    ControllerMeta controllerMeta = metaOpt.get();
  //
  //    // 步骤2：通过方法签名哈希获取端点配置
  //    String methodHash = computeMethodHash(handlerMethod);
  //    ApiEndpointLogConfig config = configManager.resolveConfig(controllerMeta.simpleName(), methodHash);
  //
  //    return new Endpoint(controllerMeta, config);
  //  }

  @Override
  public boolean preHandle(@Nonnull HttpServletRequest request,
                           @Nonnull HttpServletResponse response,
                           @Nonnull Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    Class<?> controllerClass = handlerMethod.getBeanType();
    //    Method method = handlerMethod.getMethod();
    //    String configCacheKey = controllerClass.getSimpleName() + "#" + method.getName() + "$" + method.hashCode();
    String interfaceName = controllerInterfaceNameCache.computeIfAbsent(controllerClass, k -> {
      Optional<ControllerMeta> resolved = resolveControllerMeta(controllerClass);
      return resolved.map(ControllerMeta::interfaceClass).map(Class::getName).orElse(null);
    });
    if (Strings.isBlank(interfaceName)) {
      //      logger.warn("Controller {} 未注册为可日志记录的接口", controllerClass.getName());
      return true;
    }
    //    Endpoint endpoint = resolvedConfigCache.computeIfAbsent(configCacheKey, k -> resolveConfig(request, handlerMethod, controllerClass));

    try {
      // 步骤1：通过实现类接口查找 Controller 元数据
      //      Optional<ControllerMeta> metaOpt = resolveControllerMeta(controllerClass);
      //      if (metaOpt.isEmpty()) {
      //        return true;
      //      }
      //      ControllerMeta controllerMeta = metaOpt.get();

      // 步骤2：通过方法签名哈希获取端点配置
      String methodHash = computeMethodHash(handlerMethod);
      ApiEndpointLogConfig config = configManager.resolveConfig(interfaceName, methodHash);
      if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
        return true;
      }
      //      if (endpoint == null || endpoint.controllerMeta == null || endpoint.config == null) {
      //        return true;
      //      }

      //      ApiEndpointLogConfig config = endpoint.config;
      if (!Boolean.TRUE.equals(config.getLogRequest())) {
        return true;
      }

      //      ControllerMeta controllerMeta = endpoint.controllerMeta;
      //      request.setAttribute(API_CONTROLLER_META_ATTRIBUTE, controllerMeta);

      // 步骤3：采样率检查
      if (!checkSampling(config.getSamplingRate())) {
        return true;
      }

      // 步骤4：若有条件配置，评估条件
      if (config.isConditional() && !conditionEvaluator.evaluate(config, request)) {
        return true;
      }

      // 步骤5：打印请求日志，保存配置和开始时间供 postHandle 使用
      messageBuilder.logRequest(interfaceName, config, request);

      request.setAttribute(API_CONTROLLER_META_ATTRIBUTE, interfaceName);
      request.setAttribute(API_LOG_CONFIG_ATTRIBUTE, config);
      //      request.setAttribute(API_LOG_CONFIG_ATTRIBUTE + ".startTime", System.currentTimeMillis());
      //      request.setAttribute(API_LOG_CONFIG_ATTRIBUTE + ".controllerClass", controllerClass);
    } catch (Exception e) {
      // 日志拦截器的任何异常都不应影响正常业务流程
    }

    return true;
  }

  @Override
  public void postHandle(@Nonnull HttpServletRequest request,
                         @Nonnull HttpServletResponse response,
                         @Nonnull Object handler,
                         @Nullable ModelAndView modelAndView) {
    Object configAttr = request.getAttribute(API_LOG_CONFIG_ATTRIBUTE);
    if (configAttr instanceof ApiEndpointLogConfig config && Boolean.TRUE.equals(config.getLogResponse())) {
      Object interfaceAttr = request.getAttribute(API_CONTROLLER_META_ATTRIBUTE);
      if (interfaceAttr instanceof String interfaceName) {
        Object responseBody = request.getAttribute(API_LOG_RESPONSE_BODY_ATTRIBUTE);
        messageBuilder.logResponse(interfaceName, config, request, response, responseBody);
      }
    }

    //    try {
    //      String controllerClass = "<unknown>";
    //      LabzenLogger logger;
    //      if (metaAttr instanceof ControllerMeta meta) {
    ////        controllerClass = meta.interfaceClass();
    //        logger = Loggers.getLogger(meta.interfaceClass());
    //      } else {
    //        logger = Loggers.getLogger(ex.getStackTrace()[0].getClassName());
    //      }
    //
    //      Long startTime = (Long) request.getAttribute(API_LOG_CONFIG_ATTRIBUTE + ".startTime");
    //      long costMs = startTime != null ? System.currentTimeMillis() - startTime : 0;
    //
    //      messageBuilder.logResponse(controllerClass, config, response.getStatus(), costMs);
    //    } catch (Exception e) {
    //      // 响应日志打印异常不影响正常业务流程
    //    }
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
      List<String> paramTypes = Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).toList();
      return ControllerDisposeHelper.hashControllerMethod(methodName, returnType, paramTypes);
    });
  }

  // ============================================================
  // 接口名解析
  // ============================================================

  /**
   * 通过 APT 生成的实现类接口查找 Controller 注册信息。
   * <p>
   * 遍历实现类的所有接口，取第一个在元数据注册表中存在的 ControllerMeta。
   * 由于 APT 生成的实现类始终实现唯一的 @LabzenController 接口，此方法稳定可靠。
   */
  private Optional<ControllerMeta> resolveControllerMeta(Class<?> implClass) {
    for (Class<?> interfaceClass : implClass.getInterfaces()) {
      Optional<ControllerMeta> meta = controllerRegistry.lookup(interfaceClass.getSimpleName());
      if (meta.isPresent()) {
        return meta;
      }
    }
    return Optional.empty();
  }

  // ============================================================
  // 采样
  // ============================================================

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
