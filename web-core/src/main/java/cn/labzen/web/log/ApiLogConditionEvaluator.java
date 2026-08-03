package cn.labzen.web.log;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;
import cn.labzen.web.api.log.config.ConditionGroup;
import cn.labzen.web.api.log.config.ConditionRule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Enumeration;

/**
 * API 日志条件规则评估器。
 * <p>
 * 递归遍历 {@link ConditionGroup} 条件树，对请求参数进行匹配评估。
 * <ul>
 *   <li><b>叶子节点</b>：评估单条 {@link ConditionRule}</li>
 *   <li><b>AND 节点</b>：所有子节点都满足才返回 true（短路求值）</li>
 *   <li><b>OR 节点</b>：任一子节点满足即返回 true（短路求值）</li>
 * </ul>
 *
 * @see ApiEndpointLogConfig
 * @see ConditionGroup
 * @see ConditionRule
 */
@Slf4j
public class ApiLogConditionEvaluator {

  private static final DateTimeFormatter[] DATE_FORMATTERS = {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    DateTimeFormatter.ISO_INSTANT,
    DateTimeFormatter.ISO_DATE_TIME,
    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  };

  /**
   * 评估请求是否匹配配置中的条件树。
   * <p>
   * 若 {@code config.isConditional() == false}，直接返回 {@code true}（无条件模式）。
   */
  public boolean evaluate(ApiEndpointLogConfig config, HttpServletRequest request) {
    if (!config.isConditional()) {
      return true;
    }
    if (config.isExpired()) {
      return false;
    }

    return evaluateGroup(config.getConditionGroup(), request);
  }

  /**
   * 递归评估条件组。
   */
  private boolean evaluateGroup(ConditionGroup group, HttpServletRequest request) {
    if (group == null) {
      return true;
    }

    // 叶子节点：评估单条规则
    if (group.isLeaf()) {
      return evaluateSingleRule(group.getRule(), request);
    }

    // 逻辑节点：按运算符组合子节点结果
    if (group.isGroup()) {
      return switch (group.getOperator()) {
        case AND -> evaluateAnd(group, request);
        case OR -> evaluateOr(group, request);
      };
    }

    return false;
  }

  /**
   * AND：所有子节点都满足（短路求值）。
   */
  private boolean evaluateAnd(ConditionGroup group, HttpServletRequest request) {
    for (ConditionGroup child : group.getChildren()) {
      if (!evaluateGroup(child, request)) {
        return false;
      }
    }
    return true;
  }

  /**
   * OR：任一子节点满足（短路求值）。
   */
  private boolean evaluateOr(ConditionGroup group, HttpServletRequest request) {
    for (ConditionGroup child : group.getChildren()) {
      if (evaluateGroup(child, request)) {
        return true;
      }
    }
    return false;
  }

  // ============================================================
  // 单条规则评估
  // ============================================================

  private boolean evaluateSingleRule(ConditionRule rule, HttpServletRequest request) {
    String paramValue = resolveParamValue(rule.paramName(), request);
    try {
      return switch (rule.matchType()) {
        case EXISTS -> paramValue != null;
        case EQUALS -> paramValue != null && paramValue.equals(rule.matchValue());
        case CONTAINS -> paramValue != null && paramValue.contains(rule.matchValue());
        case REGEX -> paramValue != null && paramValue.matches(rule.matchValue());
        case GREATER_THAN -> compareNumeric(paramValue, rule.matchValue()) > 0;
        case LESS_THAN -> compareNumeric(paramValue, rule.matchValue()) < 0;
        case GREATER_THAN_OR_EQUAL -> compareNumeric(paramValue, rule.matchValue()) >= 0;
        case LESS_THAN_OR_EQUAL -> compareNumeric(paramValue, rule.matchValue()) <= 0;
        case BEFORE -> compareDate(paramValue, rule.matchValue()) < 0;
        case AFTER -> compareDate(paramValue, rule.matchValue()) > 0;
      };
    } catch (Exception e) {
      logger.debug("条件匹配评估失败 [paramName={}, matchType={}]: {}", rule.paramName(), rule.matchType(), e.getMessage());
      return false;
    }
  }

  private String resolveParamValue(String paramName, HttpServletRequest request) {
    Object attrValue = request.getAttribute(paramName);
    if (attrValue != null) return attrValue.toString();
    String queryValue = request.getParameter(paramName);
    if (queryValue != null) return queryValue;
    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String name = headerNames.nextElement();
        if (name.equalsIgnoreCase(paramName)) return request.getHeader(name);
      }
    }
    return null;
  }

  private int compareNumeric(String a, String b) {
    if (a == null || b == null) throw new NumberFormatException("null");
    return new BigDecimal(a.trim()).compareTo(new BigDecimal(b.trim()));
  }

  private int compareDate(String a, String b) {
    if (a == null || b == null) throw new DateTimeParseException("null", "", 0);
    return parseToInstant(a.trim()).compareTo(parseToInstant(b.trim()));
  }

  private Instant parseToInstant(String s) {
    for (DateTimeFormatter f : DATE_FORMATTERS) {
      try {
        return Instant.from(f.parse(s));
      } catch (Exception ignored) {
      }
    }
    throw new DateTimeParseException("无法解析: " + s, s, 0);
  }
}
