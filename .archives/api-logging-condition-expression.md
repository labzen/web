# API 日志 — 条件表达式使用指南

## 概述

条件表达式（`conditionExpression`）用于指定"只有当请求参数满足特定条件时才打印日志"，避免日志量过大。

表达式由 `参数名 运算符 [值]` 组成，支持 AND / OR 逻辑连接和括号分组。

---

## 基础语法

```
参数名 运算符 [值]  [AND|OR  参数名 运算符 [值]  ...]
```

- **参数名**：客户端传递的请求参数名（query string、form body、header、request attribute）
- **运算符**：大小写不敏感，支持别名（详见下方运算符表）
- **值**：可选的匹配值。字符串值需用单引号包裹（如 `'张三'`），数值直接写（如 `100`），日期用 ISO 格式（如 `'2026-08-01'`）
- **逻辑连接**：AND（且）和 OR（或），大小写不敏感
- **括号**：`(` `)` 用于控制优先级

---

## 支持的运算符

### 字符串匹配

| 运算符 | 说明 | 示例 | 请求参数满足条件时 |
|--------|------|------|-------------------|
| `=` `==` `equals` | 精确等于 | `status = active` | `status` 参数值等于 `"active"` |
| `contains` `has` | 包含子串 | `username contains '张三'` | `username` 参数值包含子串 `"张三"` |
| `regex` `matches` `~=` | 正则匹配 | `phone regex '\d{11}'` | `phone` 参数值匹配正则 `\d{11}` |
| `exists` `present` | 参数存在 | `token exists` | `token` 参数存在（值任意） |

### 数值比较

数值匹配时，参数值和条件值均被解析为 `BigDecimal` 进行比较。

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `>` `gt` | 大于 | `amount > 100` |
| `<` `lt` | 小于 | `age < 18` |
| `>=` `gte` | 大于等于 | `score >= 60` |
| `<=` `lte` | 小于等于 | `price <= 500` |

### 日期比较

日期匹配时，参数值和条件值均被解析为 `Instant` 进行比较。
支持的日期格式：`yyyy-MM-dd HH:mm:ss`、`yyyy-MM-dd`、ISO-8601。

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `<` `before` | 早于 | `startDate before '2026-08-01'` |
| `>` `after` | 晚于 | `endDate after '2026-07-01'` |

> **注意**：`<` 和 `>` 运算符在数值比较和日期比较中共享。当参数值和条件值均为日期格式时自动按日期比较，否则按数值比较。

---

## 逻辑组合

### AND（且）

所有子条件都满足才匹配：

```
username contains '张三' AND status = active
```

### OR（或）

任一子条件满足即匹配：

```
status = admin OR status = superuser
```

### 括号分组

控制运算符优先级：

```
(username contains '张三' OR username contains '李四') AND amount > 100
```

括号内的表达式优先求值，结果再与外部条件进行逻辑运算。

---

## 参数值查找顺序

当评估条件规则时，按以下顺序查找参数值：

1. **request attribute** — 优先查找（`request.getAttribute(paramName)`）
2. **query parameter / form body** — 其次查找（`request.getParameter(paramName)`）
3. **request header** — 最后查找（大小写不敏感匹配）

---

## 配置方式

### YAML 配置

```yaml
methods:
  find:
    enabled: true
    level: DEBUG
    conditionExpression: "username contains '张三' AND status = active"
```

### 程序化 API

```java
ApiEndpointLogConfig config = new ApiEndpointLogConfig();
config.setEnabled(true);
config.setConditionExpression("username contains '张三'");
config.setTtl(Duration.ofMinutes(30));
configManager.configureMethod("UserController", "a1b2c", config);
```

---

## 约束规则

- **conditionExpression 有值 → ttl 必填**：设置了条件表达式就必须同时设置 `ttl`（存活时长），防止条件配置永久残留。配置保存失败时会输出 warn 日志
- **ttl 可选使用**：不设条件表达式时，`ttl` 是可选的，设置了则会自动计算 `expiresAt`
- **ttl 为 0**：不计算过期时间，配置永久有效
- **过期清理**：过期条件会被 `ApiLogConditionEvaluator` 在评估时自动忽略（`isExpired()` 检查）

---

## 短路求值

条件评估采用短路求值策略：

- **AND 节点**：从左到右评估子节点，第一个 `false` 立即返回，后续子节点不再评估
- **OR 节点**：从左到右评估子节点，第一个 `true` 立即返回，后续子节点不再评估

---

## 完整示例

```yaml
methods:
  # 示例1：用户名包含指定关键字时打印
  find:
    conditionExpression: "username contains '测试'"

  # 示例2：多个条件 AND 组合
  create:
    conditionExpression: "role = admin AND department contains '技术'"

  # 示例3：括号嵌套 OR
  export:
    conditionExpression: "(type = report OR type = invoice) AND size > 1000"

  # 示例4：正则匹配 + 数值比较
  search:
    conditionExpression: "phone regex '\d{11}' AND age >= 18"

  # 示例5：参数存在性检查
  privileged:
    conditionExpression: "supervisorToken exists"
```
