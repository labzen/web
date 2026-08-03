package cn.labzen.web.api.log.config;

/**
 * API 日志条件匹配类型枚举。
 * <p>
 * 定义了条件日志规则支持的全部匹配方式，分为三大类。
 * 每种类型都有对应的运算符别名，用于 YAML 表达式配置。
 * <p>
 * <b>运算符对照表：</b>
 * <table>
 *   <tr><th>类别</th><th>枚举</th><th>运算符别名</th><th>说明</th></tr>
 *   <tr><td>字符串</td><td>EQUALS</td><td>=, ==, equals</td><td>精确等于</td></tr>
 *   <tr><td>字符串</td><td>CONTAINS</td><td>contains, has</td><td>包含子串</td></tr>
 *   <tr><td>字符串</td><td>REGEX</td><td>regex, matches, ~=</td><td>正则匹配</td></tr>
 *   <tr><td>字符串</td><td>EXISTS</td><td>exists, present</td><td>参数存在即可</td></tr>
 *   <tr><td>数值</td><td>GREATER_THAN</td><td>&gt;, gt</td><td>大于</td></tr>
 *   <tr><td>数值</td><td>LESS_THAN</td><td>&lt;, lt</td><td>小于</td></tr>
 *   <tr><td>数值</td><td>GREATER_THAN_OR_EQUAL</td><td>&gt;=, gte</td><td>大于等于</td></tr>
 *   <tr><td>数值</td><td>LESS_THAN_OR_EQUAL</td><td>&lt;=, lte</td><td>小于等于</td></tr>
 *   <tr><td>日期</td><td>BEFORE</td><td>&lt;, before</td><td>早于</td></tr>
 *   <tr><td>日期</td><td>AFTER</td><td>&gt;, after</td><td>晚于</td></tr>
 * </table>
 * <p>
 * 数值匹配时，参数值和条件值都会被解析为 {@link java.math.BigDecimal} 进行比较。
 * 日期匹配时，参数值和条件值都会被解析为 ISO-8601 格式进行比较。
 *
 * @see ApiEndpointLogConfig
 * @see ConditionRule
 */
public enum MatchType {

  // ============================================================
  // 字符串匹配
  // ============================================================

  /**
   * 精确等于，运算符别名: =, ==, equals
   */
  EQUALS("=", "==", "equals"),

  /**
   * 包含子串，运算符别名: contains, has
   */
  CONTAINS("contains", "has"),

  /**
   * 正则匹配，运算符别名: regex, matches, ~=
   */
  REGEX("regex", "matches", "~="),

  /**
   * 参数存在即可，运算符别名: exists, present
   */
  EXISTS("exists", "present"),

  // ============================================================
  // 数值匹配
  // ============================================================

  /**
   * 大于，运算符别名: >, gt
   */
  GREATER_THAN(">", "gt"),

  /**
   * 小于，运算符别名: <, lt
   */
  LESS_THAN("<", "lt"),

  /**
   * 大于等于，运算符别名: >=, gte
   */
  GREATER_THAN_OR_EQUAL(">=", "gte"),

  /**
   * 小于等于，运算符别名: <=, lte
   */
  LESS_THAN_OR_EQUAL("<=", "lte"),

  // ============================================================
  // 日期匹配
  // ============================================================

  /**
   * 早于，运算符别名: <, before（与 LESS_THAN 共享 <，通过上下文区分）
   */
  BEFORE("<", "before"),

  /**
   * 晚于，运算符别名: >, after（与 GREATER_THAN 共享 >，通过上下文区分）
   */
  AFTER(">", "after");

  private final String[] aliases;

  MatchType(String... aliases) {
    this.aliases = aliases;
  }

  /**
   * 通过运算符别名查找对应的 MatchType。
   * <p>
   * 支持枚举名（如 EQUALS）和所有别名（如 =, ==, contains, gt 等）。
   *
   * @param operator 运算符字符串
   * @return 对应的 MatchType，未匹配时返回 null
   */
  public static MatchType fromOperator(String operator) {
    if (operator == null || operator.isBlank()) {
      return null;
    }
    String normalized = operator.trim().toLowerCase();

    // 先尝试枚举名
    try {
      return MatchType.valueOf(normalized.toUpperCase());
    } catch (IllegalArgumentException ignored) {
    }

    // 再尝试别名
    for (MatchType type : values()) {
      for (String alias : type.aliases) {
        if (alias.equals(normalized)) {
          return type;
        }
      }
    }

    return null;
  }
}
