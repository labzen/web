# Labzen Web API 日志功能 — 使用指南

## 快速开始

### 1. 全局默认配置（labzen.yml）

```yaml
web:
  api-log:
    enabled: false      # 全局总开关，默认 false
    level: DEBUG        # 全局默认日志级别
    request: true       # 全局默认是否打印请求日志
    response: true      # 全局默认是否打印响应日志
    sampling-rate: 1.0  # 全局默认采样率 [0.0, 1.0]
```

### 2. 为 Controller 创建 YAML 配置

在 `src/main/resources/labzen-web/` 目录下创建以 Controller **接口名**命名的 YAML 文件。

示例 — `UserController.yml`：

```yaml
general:
  enabled: true
  level: DEBUG
  logRequest: true
  logResponse: false

methods:
  create:
    enabled: true
    level: INFO
    logResponse: true
    includeParams:
      - name
      - email
```

---

## YAML 配置详解

### 文件位置

```
src/main/resources/labzen-web/{ControllerName}.yml
```

文件名必须与 Controller 接口名完全一致（如 `UserController.yml`）。

### 配置结构

| 节点 | 类型 | 说明 |
|------|------|------|
| `general` | `ApiLogConfig` | Controller 级通用配置，所有方法默认继承 |
| `methods` | `Map<String, ApiEndpointLogConfig>` | 方法级配置，key 为方法名 |

### general 节点 — 通用字段

```yaml
general:
  enabled: true       # 开关（Boolean，未设置时为 null，继承全局默认）
  level: DEBUG        # 日志级别字符串：TRACE / DEBUG / INFO / WARN / ERROR
  logRequest: true    # 是否打印请求日志（Boolean）
  logResponse: true   # 是否打印响应日志（Boolean）
  samplingRate: 1.0   # 采样率 [0.0, 1.0]（Double）
```

**注意**：所有字段均为可选的包装类型。YAML 中未配置的字段值为 null，合并时不会覆盖已设置的上级配置。

### methods 节点 — 端点级字段

`methods` 下每个 key 对应一个 API 端点方法名，**继承并覆盖** `general` 中的全部字段，并额外支持端点特有字段：

```yaml
methods:
  create:
    # --- 继承自 general 的通用字段（可选覆盖） ---
    enabled: true
    level: INFO
    logRequest: true
    logResponse: true
    samplingRate: 1.0

    # --- 端点特有字段 ---
    includeParams:           # 白名单（仅打印指定参数），与 excludeParams 互斥
      - name
      - email
    excludeParams:           # 黑名单（排除指定参数，值显示为 ***）
      - password
      - secret
    conditionExpression: >-  # 条件表达式（详见"条件日志"章节）
      username contains '张三' AND status = active
```

**配置合并规则**：`merge()` 方法使用非 null 覆盖策略。只有 YAML 中显式配置的字段（非 null）才会覆盖上级配置。
例如 `general.enabled: true`，方法级不写 `enabled` 则继承 `true`。

### methods 的 key 格式

YAML 中 methods 的 key 为 Controller 方法名（如 `create`、`find`）。
加载时 `ApiLogConfigLoader` 通过元数据注册表将方法名转换为方法签名哈希值存储。

---

## 配置方式

### 方式一：classpath YAML（推荐，静态配置）

**目录**：`src/main/resources/labzen-web/{ControllerName}.yml`

**配置合并优先级**（由低到高）：
1. 全局默认值（`WebCoreConfiguration` api-log 配置项）
2. classpath YAML 配置（general → methods）
3. 启动时程序化 API

### 方式二：启动时程序化 API

注入 `ApiLogConfigManager`：

```java
@Configuration
public class ApiLogConfiguration {

    @Autowired
    public void initApiLog(ApiLogConfigManager manager) {
        // 全局默认配置（接收 ApiLogConfig，只包含通用字段）
        ApiLogConfig globalConfig = new ApiLogConfig();
        globalConfig.setLevel(Level.INFO);
        globalConfig.setSamplingRate(0.3);
        manager.configureGlobal(globalConfig);

        // 方法级配置（接收 ApiEndpointLogConfig，包含端点特有字段）
        ApiEndpointLogConfig createConfig = new ApiEndpointLogConfig();
        createConfig.setLevel(Level.INFO);
        createConfig.setLogResponse(true);
        manager.configureMethod("UserController", "a1b2c", createConfig);
    }
}
```

**注意**：`configureMethod` 的第二个参数是方法签名哈希值（5 位十六进制），而非方法名。
哈希值可在编译期生成的 `.meta.json` 文件中查看。

### 方式三：管理查询 API

```java
@RestController
public class LogManagementController {

    @Autowired
    private ApiLogConfigManager configManager;

    // 获取所有 Controller 名列表
    @GetMapping("/admin/log/controllers")
    public List<String> getControllers() {
        return configManager.allControllerNames();
    }

    // 获取指定 Controller 的端点详情
    @GetMapping("/admin/log/endpoints")
    public List<ApiEndpointDetail> getEndpoints(@RequestParam String controllerName) {
        return configManager.getApiEndpointsDetail(controllerName);
    }
}
```

---

## 条件日志

在 `methods` 下使用 `conditionExpression` 字段配置条件表达式。

```yaml
methods:
  find:
    enabled: true
    level: DEBUG
    conditionExpression: "username contains '张三' AND status = active"
```

详细语法、运算符列表、逻辑组合、约束规则等请参见 [条件表达式使用指南](./api-logging-condition-expression.md)。

### 程序化注册

```java
ApiEndpointLogConfig config = new ApiEndpointLogConfig();
config.setEnabled(true);
config.setConditionExpression("username contains '张三'");
config.setTtl(Duration.ofMinutes(5));
configManager.configureMethod("UserController", "a1b2c", config);
```

**注意**：设置 `conditionExpression` 时必须同时设置 `ttl`，否则配置保存失败。

---

## 采样率控制

防止高 QPS 接口的日志风暴：

```yaml
general:
  samplingRate: 0.1  # 仅 10% 的请求打印日志
```

使用 `ThreadLocalRandom` 实现无锁采样。

---

## 参数过滤

### 白名单模式（includeParams 优先）

```yaml
methods:
  create:
    includeParams:
      - name
      - email
    # 仅打印 name 和 email 参数，其余参数不输出
```

### 黑名单模式

```yaml
methods:
  create:
    excludeParams:
      - password
      - secret
    # 排除 password 和 secret，值显示为 ***
```

**规则**：`includeParams` 非空时优先使用白名单，忽略 `excludeParams`。大小写不敏感匹配。

---

## 参数提取（覆盖全部 HTTP 参数方式）

`ApiLogMessageBuilder` 自动从请求中提取所有参数，不依赖 Controller 方法签名：

| 方式 | 说明 |
|------|------|
| query string（?a=1&b=2） | 通过 `getParameterMap()` 获取 |
| x-www-form-urlencoded | 通过 `getParameterMap()` 获取 |
| multipart/form-data | 文件字段打印元信息（`MultipartDetail`），普通字段通过 `getParameterMap()` 获取 |
| raw JSON | 读取 body 流，4096 字符截断 |

---

## 日志输出示例

```
POST /api/user | name=张三, email=test@example.com
GET /api/user/1 | 200 OK (35ms)
POST /api/user | 500 Internal Server Error (120ms)
```

---

## 最佳实践

1. **生产环境**：默认全局关闭（`web.api-log.enabled: false`），仅对需要排查的接口按方法级开启
2. **高 QPS 接口**：设置 `samplingRate` 为 0.1~0.3，避免日志风暴
3. **敏感数据**：利用 `excludeParams` 过滤敏感参数
4. **临时排查**：使用条件日志并设置合理的 TTL，避免忘记关闭导致磁盘写满
5. **配置分层**：`general` 层设置生产安全默认值，`methods` 层针对性开启
6. **方法哈希**：程序化 API 的 `configureMethod` 使用编译期生成的 5 位哈希值，可在 `.meta.json` 中查看
