package cn.labzen.web.log;

import cn.labzen.web.api.log.config.ConditionGroup;
import cn.labzen.web.api.log.config.ConditionRule;
import cn.labzen.web.api.log.config.MatchType;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件表达式解析器。
 * <p>
 * 将语义化条件表达式字符串解析为 {@link ConditionGroup} 树。
 * <p>
 * 示例：
 * <pre>{@code
 *   "(username contains '张三' AND status = active) OR amount > 1000"
 *   → ConditionGroup(OR, [
 *       ConditionGroup(AND, [leaf(username,contains,张三), leaf(status,=,active)]),
 *       leaf(amount,>,1000)
 *     ])
 * }</pre>
 */
public final class ConditionExpressionParser {

  private ConditionExpressionParser() {
  }

  /**
   * 将条件表达式字符串解析为 {@link ConditionGroup} 树。
   * <p>
   * 支持 AND / OR 逻辑连接和括号分组。使用递归下降解析算法。
   *
   * @param expression 条件表达式字符串，如 {@code "(username contains '张三' AND status = active) OR amount > 1000"}，
   *                   为 null 或空白时返回 null
   * @return 解析后的条件树根节点，解析失败或输入为空时返回 null
   * @see MatchType#fromOperator(String)
   */
  public static ConditionGroup parse(String expression) {
    if (expression == null || expression.isBlank()) {
      return null;
    }
    List<String> tokens = tokenize(expression.trim());
    if (tokens.isEmpty()) {
      return null;
    }
    int[] pos = {0};
    return parseOrExpr(tokens, pos);
  }

  private static ConditionGroup parseOrExpr(List<String> tokens, int[] pos) {
    List<ConditionGroup> children = new ArrayList<>();
    children.add(parseAndExpr(tokens, pos));
    while (pos[0] < tokens.size() && "OR".equalsIgnoreCase(tokens.get(pos[0]))) {
      pos[0]++;
      children.add(parseAndExpr(tokens, pos));
    }
    return children.size() == 1 ? children.getFirst() : ConditionGroup.or(children);
  }

  private static ConditionGroup parseAndExpr(List<String> tokens, int[] pos) {
    List<ConditionGroup> children = new ArrayList<>();
    children.add(parsePrimary(tokens, pos));
    while (pos[0] < tokens.size() && "AND".equalsIgnoreCase(tokens.get(pos[0]))) {
      pos[0]++;
      children.add(parsePrimary(tokens, pos));
    }
    return children.size() == 1 ? children.getFirst() : ConditionGroup.and(children);
  }

  private static ConditionGroup parsePrimary(List<String> tokens, int[] pos) {
    if (pos[0] >= tokens.size()) {
      return null;
    }
    String token = tokens.get(pos[0]);
    if (")".equals(token) || "OR".equalsIgnoreCase(token) || "AND".equalsIgnoreCase(token)) {
      return null;
    }
    if ("(".equals(token)) {
      pos[0]++;
      ConditionGroup inner = parseOrExpr(tokens, pos);
      if (pos[0] < tokens.size() && ")".equals(tokens.get(pos[0]))) {
        pos[0]++;
      }
      return inner;
    }
    ConditionRule rule = parseSingleCondition(tokens, pos);
    return rule != null ? ConditionGroup.leaf(rule) : null;
  }

  private static ConditionRule parseSingleCondition(List<String> tokens, int[] pos) {
    if (pos[0] >= tokens.size()) {
      return null;
    }
    String paramName = tokens.get(pos[0]++);
    if (pos[0] >= tokens.size()) {
      return null;
    }
    String maybeOp = tokens.get(pos[0]);
    if ("AND".equalsIgnoreCase(maybeOp) ||
        "OR".equalsIgnoreCase(maybeOp) ||
        "(".equals(maybeOp) ||
        ")".equals(maybeOp)) {
      pos[0]--;
      return null;
    }
    String operator = tokens.get(pos[0]++);
    String matchValue = null;
    if (pos[0] < tokens.size()) {
      String next = tokens.get(pos[0]);
      if (!"AND".equalsIgnoreCase(next) && !"OR".equalsIgnoreCase(next) && !"(".equals(next) && !")".equals(next)) {
        matchValue = stripQuotes(tokens.get(pos[0]++));
      }
    }
    MatchType matchType = MatchType.fromOperator(operator);
    if (matchType == null) {
      return null;
    }
    if (matchType == MatchType.EXISTS) {
      return new ConditionRule(matchType, paramName, null);
    }
    return new ConditionRule(matchType, paramName, matchValue);
  }

  private static List<String> tokenize(String expression) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuote = false;
    for (int i = 0; i < expression.length(); i++) {
      char c = expression.charAt(i);
      if (c == '\'') {
        inQuote = !inQuote;
        current.append(c);
      } else if (c == '(' || c == ')') {
        if (!current.isEmpty()) {
          tokens.add(current.toString());
          current.setLength(0);
        }
        tokens.add(String.valueOf(c));
      } else if (c == ' ' && !inQuote) {
        if (!current.isEmpty()) {
          tokens.add(current.toString());
          current.setLength(0);
        }
      } else {
        current.append(c);
      }
    }
    if (!current.isEmpty()) {
      tokens.add(current.toString());
    }
    return tokens;
  }

  private static String stripQuotes(String value) {
    if (value == null || value.length() < 2) {
      return value;
    }
    char first = value.charAt(0), last = value.charAt(value.length() - 1);
    if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }
}
