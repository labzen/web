# Labzen Web API 日志功能 — 实现机制说明

## 概述

API 日志功能为 Labzen Web 框架提供零侵入的请求/响应/异常全生命周期日志记录能力。
通过 `ApiLogInterceptor`（拦截器）+ `ApiLogResponseAdvice`（响应体捕获器）协作捕获请求和响应，
辅以编译期 APT 生成的 Controller 元数据注册表消除运行时反射开销。

---

## 核心架构

### 请求处理链路

```
HTTP Request
  → LabzenExceptionCatchingFilter（Filter，兜底异常）
    → ApiLogInterceptor.preHandle()
      → resolveControllerMeta()          通过实现类接口查找元数据（带缓存）
      → computeMethodHash()              方法签名 MD5 哈希（带缓存）
      → ApiLogConfigManager.resolveConfig()  缓存 + 三层配置合并
      → 采样率检查（samplingRate）
      → 条件评估（ApiLogConditionEvaluator，短路求值）
      → ApiLogMessageBuilder.logRequest()  打印请求日志
    → Controller 方法执行
  → LabzenRestResponseBodyAdvice.beforeBodyWrite()  （响应格式化）
  → ApiLogResponseAdvice.beforeBodyWrite()          （捕获原始响应体 → request attribute）
  → ApiLogInterceptor.postHandle()
    → 从 request attribute 取响应体
    → ApiLogMessageBuilder.logResponse()  打印响应日志

异常路径：
  → LabzenHandlerExceptionResolver    → ApiLogMessageBuilder.logException()
  → LabzenExceptionCatchingFilter     → ApiLogMessageBuilder.logException()
```

### 配置三层合并

```
启动时程序化 API（configureGlobal / configureMethod） ← 最高优先级
> classpath YAML（resources/labzen-web/{ControllerName}.yml）
> 全局默认（WebCoreConfiguration api-log 配置项）
```

合并通过 `ApiLogConfig.merge()` 实现：`@Data` 生成的字段均为包装类型（`Boolean`/`Double`），`null` 表示未设置。
`merge()` 方法遍历每个字段，仅用 override 中的非 null 值覆盖原值。
`ApiEndpointLogConfig.merge()` 在父类基础上补充端点特有字段。

---

## 模块划分与文件清单

### web-api 模块 — 配置模型与元数据

| 文件 | 说明 |
|------|------|
| `config/ApiLogConfig.java` | 通用日志配置。字段为包装类型（`Boolean`/`Double`），`null` 表示未设置。`level` 为 String（YAML 直接映射），`getLevel()` 返回 `Level` 枚举。`merge()` 实现非 null 覆盖 |
| `config/ApiEndpointLogConfig.java` | 端点日志配置（继承 ApiLogConfig）。增加 `conditionExpression`（YAML 直接映射）、参数过滤、条件触发等端点特有字段。`merge()` 补充端点字段覆盖 |
| `config/ConditionGroup.java` | 条件树节点（自描述 AND/OR），支持 `isLeaf()` 和 `isGroup()` 判断节点类型 |
| `config/ConditionRule.java` | 单条条件规则（matchType + paramName + matchValue） |
| `config/MatchType.java` | 10 种匹配类型枚举，含 `fromOperator()` 工厂方法支持运算符别名 |
| `config/LogicOperator.java` | AND / OR 枚举 |
| `registry/ControllerMeta.java` | Controller 元数据（接口 Class、简单名、方法 Map） |
| `registry/ControllerMethodMeta.java` | 方法元数据（hash、方法名、HTTP 方法、URL 模式、参数类型列表） |
| `definition/Constants.java` | 常量定义：场景标识、请求属性键、配置 key |

### web-core 模块 — 运行时实现

| 文件 | 说明 |
|------|------|
| `LoggableControllerMetaRegistry.java` | 元数据注册表（`SmartInitializingSingleton`，`@Order(Integer.MIN_VALUE+1000)`）。`afterSingletonsInstantiated()` 时从 `META-INF/labzen/*.meta.json` 加载。注册三个 key：hash、方法名、HTTP方法+URL |
| `ApiLogConfigLoader.java` | YAML 配置加载器。SnakeYAML 直接映射到 `YamlFile`（`ApiLogConfig`/`ApiEndpointLogConfig`）。加载时解析 `conditionExpression` → `ConditionGroup`。验证元数据注册表 |
| `ApiLogConfigManager.java` | 配置统一入口（`SmartInitializingSingleton`，`@Order(Integer.MIN_VALUE+2000)`，保证在 Registry 之后初始化）。三层合并 + 缓存 + 缓存失效。提供 `configureGlobal`、`configureMethod` 程序化 API，以及 `allControllerNames`、`getApiEndpointsDetail` 查询 API |
| `ApiLogConditionEvaluator.java` | 条件树递归评估器，短路求值。参数值解析顺序：request attribute → query parameter → header |
| `ConditionExpressionParser.java` | 递归下降表达式解析器，`conditionExpression` 字符串 → `ConditionGroup` 树。支持 AND/OR/括号/单引号 |
| `ApiLogMessageBuilder.java` | 日志消息构建：请求参数提取（query/form/multipart/JSON body）、参数过滤（白名单/黑名单）、文件元信息、结构化日志输出（LabzenLogger） |
| `ApiLogInterceptor.java` | HandlerInterceptor。preHandle 执行决策链（元数据查找 → 配置解析 → 采样 → 条件评估 → 请求日志）。postHandle 输出响应日志。接口名和哈希均带缓存 |
| `ApiLogResponseAdvice.java` | ResponseBodyAdvice（`@Order(2000)`），在 LabzenRestResponseBodyAdvice 之后执行。只负责捕获原始响应体存入 `request.setAttribute(RESPONSE_RESULT_BODY_ATTRIBUTE)` |
| `bean/YamlFile.java` | SnakeYAML 顶层映射结构（general + methods） |
| `bean/ApiEndpointDetail.java` | 管理页面展示用端点详情 record |
| `bean/MultipartDetail.java` | 文件上传详情 record（含 toString 格式化） |

### web-processor 模块 — 编译期生成

| 文件 | 说明 |
|------|------|
| `MetadataGenerateProcessor.java` | 为每个 @LabzenController 接口生成 `.meta.json`（含方法签名 MD5 前 5 位哈希），输出到 `META-INF/labzen/{ControllerName}.meta.json` |
| `InternalProcessor.java` | sealed 接口新增 `MetadataGenerateProcessor` permits |

### Spring 集成

| 文件 | 说明 |
|------|------|
| `LabzenWebComponentRegistrar.java` | 注册 LoggableControllerMetaRegistry、ApiLogConfigManager、ApiLogMessageBuilder、ApiLogInterceptor、ApiLogResponseAdvice |
| `WebCoreConfiguration.java` | `api-log.enabled`/`api-log.level`/`api-log.request`/`api-log.response`/`api-log.sampling-rate` 全局默认配置项 |

---

## 关键实现细节

### 1. 接口名解析（避免实现类名不匹配）

`ApiLogInterceptor.resolveControllerMeta()` 通过 `implClass.getInterfaces()` 遍历找到第一个在元数据注册表中存在的接口名。
结果缓存于 `ConcurrentHashMap`，同一实现类只计算一次。

### 2. YAML 配置直接映射（无中间 Bean）

`ApiLogConfig.level` 为 String 类型（SnakeYAML 直接映射 YAML 中的 `level: DEBUG`），`setLevel(String)` 同时设置 `resolvedLevel` 枚举值。
`ApiEndpointLogConfig.conditionExpression` 为 String 类型（YAML 直接映射），`ApiLogConfigLoader` 调用 `ConditionExpressionParser.parse()` 注入 `ConditionGroup`。
`YamlFile` 是独立的顶层映射 Bean，`general` → `ApiLogConfig`，`methods` → `Map<String, ApiEndpointLogConfig>`。

### 3. 日志决策链（ApiLogInterceptor.preHandle）

```
① resolveControllerMeta()     通过实现类接口查找元数据（带缓存，返回接口全限定名）
② computeMethodHash()         方法签名 MD5 前 5 位（带缓存）
③ resolveConfig()             三层配置合并 → ApiEndpointLogConfig
④ config.enabled 检查         未启用则跳过
⑤ checkSampling()             采样率检查（ThreadLocalRandom）
⑥ 条件评估                    若 isConditional() → ApiLogConditionEvaluator.evaluate()（短路求值）
⑦ logRequest()                打印请求日志 + 保存配置到 request attribute
```

### 4. 条件表达式解析与评估

**解析**：`ConditionExpressionParser.parse()` 递归下降算法，支持 AND/OR/括号嵌套/单引号值，将 `"username contains '张三' AND status = active"` 转为 `ConditionGroup` 树。

**评估**：`ApiLogConditionEvaluator.evaluateGroup()` 递归遍历条件树，叶子节点按 MatchType 匹配请求参数（优先级：request attribute → query parameter → header），AND/OR 节点短路求值。

### 5. 参数提取（覆盖全部 HTTP 参数方式）

`ApiLogMessageBuilder.extractRequestParams()` 从 `request.getParameterMap()` 获取 query string / form 参数，
multipart 文件字段通过 `request.getParts()` 记录元信息（`MultipartDetail`），JSON body 通过 `request.getReader()` 读取并截断。

### 6. 响应体捕获（ApiLogResponseAdvice → postHandle）

`ApiLogResponseAdvice`（`@Order(2000)`）在 `LabzenRestResponseBodyAdvice` 之后执行，
将原始 body 存入 `request.setAttribute(RESPONSE_RESULT_BODY_ATTRIBUTE, body)`。
`ApiLogInterceptor.postHandle` 从中取出传给 `ApiLogMessageBuilder.logResponse()`。

`logResponse` 中若 body 为 `Response` 类型则序列化为 JSON 作为结构化日志的 content 字段输出，否则直接拼接字符串。

### 7. 配置合并机制

`ApiLogConfig.merge()` 使用包装类型 + 非 null 覆盖策略：`Boolean`/`Double`/`String` 默认值为 null，YAML 未配置的字段保持 null，`merge` 时只有非 null 值才覆盖。`value()` 方法：`overrideValue != null ? overrideValue : originalValue`。

### 8. 配置缓存与失效

`ApiLogConfigManager.resolveConfig()` 使用 `ConcurrentHashMap.computeIfAbsent()` 缓存合并结果。
`configureMethod()` 调用 `resolvedConfigCache.remove(key)` 失效对应缓存。

### 9. 线程安全

| 组件 | 并发策略 |
|------|---------|
| `LoggableControllerMetaRegistry.registry` | `afterSingletonsInstantiated` 一次性写入后只读 |
| `ApiLogConfigManager.yamlConfigs` | `ConcurrentHashMap` |
| `ApiLogConfigManager.programmaticConfigs` | `ConcurrentHashMap` |
| `ApiLogConfigManager.resolvedConfigCache` | `ConcurrentHashMap` |
| `ApiLogInterceptor.controllerInterfaceNameCache` | `ConcurrentHashMap` |
| `ApiLogInterceptor.methodHashCache` | `ConcurrentHashMap` |
| 过期清理调度器 | `ScheduledExecutorService` 单线程（daemon） |
| 采样随机数 | `ThreadLocalRandom` |

### 10. 日志格式

请求日志通过 `doRequestLogging` 使用 LabzenLogger 结构化 API 输出，场景标记为 `Scenes.REQUEST`。
响应日志通过 `doResponseLogging` 输出，场景标记为 `Scenes.RESPONSE`，若 body 为 `Response` 类型则 JSON 序列化后通过 `.json(content)` 附加。

```
POST /api/user | name=张三, email=test@example.com
GET /api/user/1 | 200 OK (35ms)
POST /api/user | 500 Internal Server Error (120ms)
```
