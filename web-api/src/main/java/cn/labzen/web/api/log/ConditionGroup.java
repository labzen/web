package cn.labzen.web.api.log;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件规则组（树形节点）。
 * <p>
 * 自描述 AND/OR 嵌套关系：
 * <ul>
 *   <li><b>叶子节点</b>：{@code rule != null, operator == null, children.isEmpty()}</li>
 *   <li><b>逻辑节点</b>：{@code rule == null, operator != null, children 非空}</li>
 * </ul>
 * <p>
 * 示例：{@code (A AND B) OR C}
 * <pre>{@code
 *   ConditionGroup(OR, [
 *     ConditionGroup(AND, [
 *       ConditionGroup(ruleA),
 *       ConditionGroup(ruleB)
 *     ]),
 *     ConditionGroup(ruleC)
 *   ])
 * }</pre>
 *
 * @param operator 逻辑运算符（叶子节点为 null）
 * @param children 子组（叶子节点为空列表）
 * @param rule     单条条件规则（逻辑节点为 null）
 */
@Data
public class ConditionGroup {

  private LogicOperator operator;
  private List<ConditionGroup> children = new ArrayList<>();
  private ConditionRule rule;

  // ============================================================
  // 工厂方法
  // ============================================================

  /**
   * 创建叶子节点
   */
  public static ConditionGroup leaf(ConditionRule rule) {
    ConditionGroup g = new ConditionGroup();
    g.rule = rule;
    return g;
  }

  /**
   * 创建逻辑节点
   */
  public static ConditionGroup group(LogicOperator operator, List<ConditionGroup> children) {
    ConditionGroup g = new ConditionGroup();
    g.operator = operator;
    g.children = new ArrayList<>(children);
    return g;
  }

  /**
   * 创建 AND 节点
   */
  public static ConditionGroup and(List<ConditionGroup> children) {
    return group(LogicOperator.AND, children);
  }

  /**
   * 创建 OR 节点
   */
  public static ConditionGroup or(List<ConditionGroup> children) {
    return group(LogicOperator.OR, children);
  }

  // ============================================================
  // 便捷方法
  // ============================================================

  public boolean isLeaf() {
    return rule != null;
  }

  public boolean isGroup() {
    return operator != null && !children.isEmpty();
  }
}
