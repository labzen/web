package cn.labzen.web.api.log.config;

/**
 * 单条条件匹配规则（不可变 record）。
 * <p>
 * 描述一个请求参数值需满足的匹配条件。多条规则可通过 AND（同一条件组内）或 OR（不同条件组之间）组合。
 * <p>
 * <b>数值比较：</b>{@code matchValue} 会被解析为 {@link java.math.BigDecimal}。
 * <b>日期比较：</b>{@code matchValue} 会被解析为 ISO-8601 格式（如 {@code 2026-07-31T14:30:00}）。
 *
 * @param matchType  匹配类型
 * @param paramName  要检查的请求参数名
 * @param matchValue 匹配值
 * @see MatchType
 * @see ApiEndpointLogConfig
 */
public record ConditionRule(MatchType matchType, String paramName, String matchValue) {
}
