The user wants me to read the entire project code and find hidden bugs and security vulnerabilities. This is a large task that requires exploring the codebase thoroughly. Let me use the code-explorer subagent to do a comprehensive scan of the project.

Let me launch multiple agents to explore different modules in parallel.
The three agents have completed their thorough analysis. Let me compile the results into a comprehensive, well-organized report for the user.
# Labzen.Web 全项目代码审计报告

基于对 `web-api`（45个文件）、`web-core`（30个文件）、`web-processor`（40个文件）全部源码的逐行审查，发现以下问题：


> 待解决：
> - 文件上传相关：重新构建机制
> - 高位安全漏洞解决到6

---

## 🔴 严重/高危 — 安全漏洞

### 1. 文件上传无安全约束 — 可能导致 RCE
`FileController.java:24-25` — `MultipartFile` 直接接收上传文件，**无大小限制、无类型校验、无文件名消毒**。可上传 `.jsp`/`.exe` 恶意文件导致远程代码执行。

### 2. 文件上传仅验证扩展名 — MIME 类型未校验
`StandardUploadedFile.java:35-57` — 仅通过文件名扩展名判断文件类型，攻击者可将 webshell 重命名为 `.png` 绕过检查。

### 3. 文件路径遍历 — `store()` 方法未校验目标路径
`StandardUploadedFile.java:80-91` — `store(Path path)` 直接接受任意路径写入，无规范化/白名单校验，攻击者可通过 `../` 写入任意目录。

### 4. 文件下载路径遍历 — `FileResult` 无路径校验
`FileDownloadResponseFormatter.java:55-71` — 直接使用 `fileResult.value()` 打开文件流返回客户端，无路径校验，可读取服务器任意文件。

### 5. 异常信息泄露 — 内部错误直接暴露给客户端
- `LabzenExceptionCatchingFilter.java:88-97` — `e.getMessage()` 和根因异常消息直接返回客户端，可能泄露 SQL 语句、类名、文件路径
- `LabzenHandlerExceptionResolver.java:144-180` — Spring MVC 异常消息（参数名、类型转换细节）直接输出

### 6. XSS 风险 — Accept Header 原样设为 Content-Type
- `LabzenExceptionCatchingFilter.java:116-120` — 直接将客户端 `Accept` Header 值设为响应 `Content-Type`，无任何校验，可被利用做 Content-Type Sniffing / 反射型 XSS
- `LabzenHandlerExceptionResolver.java:237-243` — 同类问题

### 7. SQL 注入风险 — 排序字段名未白名单校验
- `Order.java:10` — `name` 字段直接来自前端请求参数
- `PageableResolver.java:109-124` — 排序字段名从请求参数提取后无白名单校验，若后端拼接到 `ORDER BY` 子句将导致 SQL 注入

### 8. 导出格式参数未校验
`FileController.java:21-22` — `@PathVariable String format` 无白名单或正则校验，可被传入 `../` 等路径遍历字符。

---

## 🔴 严重/高危 — 隐藏 Bug

### 9. `StandardResourceService` 默认静默成功 — 灾难性逻辑错误
`StandardResourceService.java:26-85` — 所有 6 个 CRUD 方法默认返回 `Results.success()`。**忘记覆写时，`remove()` 不删除记录却返回成功，`create()` 不创建记录却返回成功。** 应抛出 `UnsupportedOperationException`。

### 10. `deferredControllers` 未清除 — 编译无限循环
`LabzenWebProcessor.java:86-97,116-119` — `getAndResetDeferredControllers()` 名为"重置"但**从未清除**集合。失败的控制器反复被重新处理，形成编译无限循环，严重时卡死编译。

### 11. `Pageable.to()` 默认返回 null — 静默空指针
`Pageable.java:104-115` — 三个转换方法默认返回 `null`，实现类忘记覆写时，调用方解包触发 `NullPointerException`。

### 12. `request.getAttribute()` 可能返回 null — NPE
`StandardResultResponseFormatter.java:45-47` / `UnexpectedResponseFormatter.java:36-37`:
```java
String requestTime = request.getAttribute(REST_REQUEST_TIME).toString(); // NPE!
```

### 13. 资源泄漏 — OutputStream 未在 try-with-resources 中
`StandardUploadedFile.java:85-91`:
```java
FileCopyUtils.copy(is, Files.newOutputStream(path)); // OutputStream 未被管理
```
若 `copy()` 抛异常，OutputStream 不会关闭，Windows 上文件句柄被锁定。

### 14. `PageConverterHolder` 线程安全 + 空指针风险
`PageConverterHolder.java:7-15` — `converter` 是普通 static 字段，**无 `volatile`，无同步**。一个线程的 set 对其他线程不可见；`getConverter()` 可返回 null 导致下游 NPE。

### 15. 递归 `findRootCause` 可能 StackOverflow
`LabzenExceptionCatchingFilter.java:103-108` — 异常链若存在循环引用（`A→B→A`），递归将无限执行导致 `StackOverflowError`。

### 16. 模板参数索引越界 — APT 崩溃
`ClassCreator.java:265-269` — `{#parameterN}` 占位符的索引 `N` 未做边界检查，模板被篡改或参数不匹配时直接 `IndexOutOfBoundsException`。

### 17. `TypeName` 强转 `ClassName` — ClassCastException
`ClassCreator.java:287` / `ElementAnnotation.java:30` — `annotation.getType()` 返回 `TypeName`，直接强转为 `ClassName`，遇到参数化类型时崩溃。

### 18. 方法签名使用简名 — 重载方法被错误合并
`EvaluateMethodsProcessor.java:136-142` — 使用 `Utils.getSimpleName(returnType)` 构建签名，`java.util.Date` 和 `java.sql.Date` 会被视为相同类型，方法被错误合并。

### 19. `assert` 在生产环境失效
`SimplestControllerInterfaceGenericsEvaluator.java:22` / `StandardControllerInterfaceGenericsEvaluator.java:22` — 用 `assert` 检查空列表，生产 JVM 默认不启用 `-ea`，`arguments.getFirst()` 直接 `NoSuchElementException`。

---

## 🟡 中危问题

| # | 问题 | 位置 |
|---|------|------|
| 20 | `FileResult.filename()` 可能含 CRLF 字符，设置 `Content-Disposition` 时导致 HTTP Header Injection | `FileResult.java:16-34` |
| 21 | `pageSize` 无上限限制，攻击者可传 `Integer.MAX_VALUE` 造成 DoS | `PageableResolver.java:50-66` |
| 22 | 批量删除接口 `removes()` 无数量上限 | `StandardController.java:92-93` |
| 23 | 错误响应统一返回 HTTP 200，WAF/监控无法检测异常 | `LabzenExceptionCatchingFilter.java:121` |
| 24 | `LabzenHandlerExceptionResolver` 手动 new 创建，`@Resource` 注入的 `converters` 为 null | `LabzenHandlerExceptionResolver.java:61-62` |
| 25 | `LabzenWebContextInitializer` 只捕获 `ClassNotFoundException`，`LinkageError` 等会导致启动失败 | `LabzenWebContextInitializer.java:48-56` |
| 26 | `IllegalAccessException` 被静默忽略，字段复制失败无感知 | `PageableDelegator.java:139-151` |
| 27 | SPI 加载的 `ResponseFormatter` 无安全校验，恶意实现可拦截修改响应 | `CompositeResponseFormatter.java:51-54` |
| 28 | `@APIVersion` 的 `value()` 值直接用于构建路径，无校验 | `LabzenVersionedApiRequestMappingHandlerMapping.java:61-67` |
| 29 | ThreadLocal 内存泄漏 — `CONTEXT_HOLDER` 从未调用 `remove()` | `LabzenWebProcessor.java:43` |
| 30 | `readContent()` 丢失换行符，多行模板生成错误代码 | `ClassCreator.java:116-126` |
| 31 | 配置值（`classNameSuffix`、`apiVersionHeaderVND`）无校验，恶意值可注入生成代码 | `Config.java:36-94` |
| 32 | 文件为空时抛出 500 错误码，语义应为 400 | `StandardUploadedFile.java:37` |
| 33 | `ElementParameter.equals` 不考虑 `name`，参数名更新被跳过 | `ElementParameter.java:36-44` |
| 34 | `PrepareProcessor` 对非法输入仅警告不中断，后续处理会产生无意义错误 | `PrepareProcessor.java:29-35` |

---

## 🟢 设计缺陷（需关注）

| # | 问题 | 说明 |
|---|------|------|
| 35 | `@Call(method=字符串)` 重构不安全 | 方法名硬编码，编译期无法校验存在性 |
| 36 | 多个注解为 SOURCE 保留 | `@Catching`、`@Crypto`、`@Monitor`、`@Threshold` 等运行时不可见 |
| 37 | `StandardController` 的 `ID` 泛型与 `\d{1,19}` 正则不兼容 | `UUID`/`String` 类型的 ID 永远匹配不到路由 |
| 38 | `@Crypto` 未实现 | 开发者误信响应已加密，**敏感数据明文暴露** |
| 39 | `@Threshold` 未实现 | API 无限流熔断保护，可被 DDoS |
| 40 | `FileControllerInterfaceGenericsEvaluator` 返回空列表 | `FileController` 的泛型不会被注入为依赖字段，运行时 NPE |
| 41 | `ValueResult.value` 为 `Object` 类型 | 敏感字段无过滤直接序列化到 JSON |

---

## 🎯 优先修复建议（TOP 5）

1. **文件上传/下载安全加固** — 添加大小限制、MIME 校验、路径规范化、白名单机制（#1, #2, #3, #4）
2. **`StandardResourceService` 默认实现改为抛异常** — 当前静默成功是最危险的逻辑错误（#9）
3. **`deferredControllers` 清除逻辑** — 避免编译无限循环（#10）
4. **异常响应不再暴露内部信息** — 生产环境返回通用提示，Accept Header 限定为 JSON（#5, #6）
5. **排序字段名白名单校验** — 防止 SQL 注入（#7）