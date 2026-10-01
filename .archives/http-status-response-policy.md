# HTTP 状态码响应口径

## 1. 当前口径（框架保证）

**框架输出的 API 响应，HTTP 状态码一律保持容器默认值（200），真实状态码一律承载于响应信封的 `code` 字段。**

| 出口 | 位置 | HTTP 状态码 |
| --- | --- | --- |
| 异常解析器（绑定失败、校验失败、请求体无法解析等） | `LabzenHandlerExceptionResolver` | 不设置（200），真实状态码在信封 `code` |
| 异常捕捉过滤器（逃逸到 Filter 的异常） | `LabzenExceptionCatchingFilter` | 不设置（200），真实状态码在信封 `code` |
| 业务异常（`RequestException`）响应体增强 | `LabzenRestResponseBodyAdvice#handleLabzenRequestException` | 不设置（200），真实状态码在信封 `code` |

唯一例外（不属于异常信封链路）：`LabzenRestRequestHandlerInterceptor#preHandle` —— 当
`web.core.api-version.carrier=HEADER` 且 `web.core.api-version.header-accept-forced=true` 时，
请求的 `Accept` 头不带 `application/vnd` 会返回 **HTTP 406**，响应体为 `text/plain`。

## 2. 消费方（前端）注意事项

- 判断请求结果请以**响应信封的 `code` 字段**为准，不要依赖 HTTP 状态码。
- 若将来提供「返回真实 HTTP 状态码」的配置项，其**默认值保持"关闭"**，即维持当前「200 + 信封 `code`」的口径。
- 若将来支持运行期切换该配置：切换前后，前端的判断逻辑必须同步调整，否则会因 HTTP 状态码变化而全部失效。

## 3. 后续计划（TODO）

计划在 `labzen.yml` 中增加一个全局配置项，用于在两种口径之间切换：

- **真实状态**：HTTP 状态码使用真实的业务/错误状态码；
- **恒定 200**：HTTP 状态码一律 200，真实状态码承载于信封 `code`（即当前口径）。

要求：

1. 配置生效后，上文三处信封出口（不含版本头校验拒绝）统一遵守同一策略。
2. 默认值保持「恒定 200」。
3. 配置命名建议（待定）：`web.core.response.http-status`，取值 `REAL` / `ENVELOPE`；或等价的布尔项。
4. 切换为「真实状态」属破坏性变更，需前端同步调整。

## 4. 影响面（供实现时参考）

- `web-core`：`LabzenHandlerExceptionResolver`（`responseWithMessage` / `responseWithData` / `responseNoData`）、
  `LabzenExceptionCatchingFilter#sendMessage`、`LabzenRestResponseBodyAdvice#handleLabzenRequestException`。
- `web-core`：`WebCoreConfiguration` 新增配置项。
- 文档：`resource-body-binding.md` 第 6 节关于状态码的说明需同步。

## 5. 验收点

- 配置为「真实状态」时：绑定失败、校验失败、请求体解析失败、业务异常、逃逸异常，HTTP 状态码均为真实状态码。
- 配置为「恒定 200」时：上述场景 HTTP 状态码均为 200，真实状态码仅出现在信封 `code`。
- 同一场景在两种配置下的响应信封结构保持一致。
