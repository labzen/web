package cn.labzen.web.apt.suggestion;

/**
 * 代码生成建议
 * <p>
 * 评价器通过 Suggestion 向处理器表达代码生成意图。
 * 分为四种类型：
 * <ul>
 *   <li>AppendSuggestion - 添加元素（字段、注解）</li>
 *   <li>RemoveSuggestion - 移除元素</li>
 *   <li>ReplaceSuggestion - 替换或修改元素</li>
 *   <li>DiscardSuggestion - 废弃整个方法</li>
 * </ul>
 */
public sealed interface Suggestion
  permits AppendSuggestion, DiscardSuggestion, RemoveSuggestion, ReplaceSuggestion {
}
