# JSON 请求体绑定 —— 需求与约束（来自消费方 paragon）

> **给 web 侧阅读**（人 / AI 均可）。
> **状态更新（2026-10-01）**：`application/json` 入参**已落地** —— web 侧实现 `@ResourceBody`（编译期按 `processor.resource-binding=FORM|JSON` 落为 `@ModelAttribute` / `@RequestBody`，显式 Spring 绑定注解优先）。消费方评估结论：**载体符合本文的硬要求**。
> 因此本文件现在的定位是：① 契约对照基线（**今后改动绑定 / 反序列化 / 异常响应相关行为时，这些保证不能破**）；② **§10 的待办条目**（消费方评估后提出的三项诉求）。
> 读完本文即可评估改动影响，无需再去看 paragon 的代码。

---

## 0. 现状（已核实，供你定位）

| 项 | 事实 |
| --- | --- |
| JSON 绑定 | 由 `@ResourceBody` 承载（`@Target(PARAMETER)` / `RetentionPolicy.CLASS`）：全仓**没有手写的 `@RequestBody`**，绑定方式在编译期由配置决定 |
| 现有绑定方式 | 基接口 `StandardController` / `FileController` 用 `@ModelAttribute`（资源 bean）/ `@RequestParam` / `@PathVariable` |
| 代码生成 | `web-processor` → `LabzenWebProcessor`（`@SupportedAnnotationTypes(@LabzenController)`）内部是**有序阶段链**：`PrepareProcessor` → `ReadSourceProcessor` → `ReadAnnotationsProcessor` → `EvaluateFieldsProcessor` → `EvaluateMethodsProcessor` → `CreativeProcessor`（JavaPoet 生成 `*ControllerImpl`）；另有 `MetadataGenerateProcessor` 输出运行时元数据 |
| 注解改写挂点 | `ReadAnnotationsProcessor` / `EvaluateMethodsProcessor` 已有 `Suggestion` 机制（`Append` / `Remove` / `Replace` / `Discard`）—— **新增或改写绑定注解的现成扩展点** |
| 运行时挂点 | `LabzenWebConfigurer`（`WebMvcConfigurer`）；已有自定义 `HandlerMethodArgumentResolver` 先例（`PageableCompatibleArgumentResolver`）；`LabzenExceptionCatchingFilter`（统一异常 → 信封） |

---

## 1. 为什么要这些约束（契约 30 秒版）

paragon 用「**缺省 / 显式 null / 有值**」三态语义实现部分更新：

| 报文 | 语义 |
| --- | --- |
| 字段**缺省** | 不动该字段（不改列） |
| 字段**显式 `null`** | 清空该列（写 `NULL`） |
| 字段**有值** | 写入 |

为区分前两者，paragon 会用 Jackson 的 `BeanDeserializerModifier` 记录「**本次报文里出现过的属性名**」（`providedFields`），据此逐字段判断"缺省"还是"显式 null"。

⇒ **前提有两条**：反序列化结果必须**保留** `{}` 与 `{"x":null}` 的差异；且必须使用**应用侧的 `ObjectMapper`**。

---

## 2. 🔴 硬要求（红线，4 条）

| # | 要求 | 违反后果 |
| --- | --- | --- |
| 1 | **不得塌陷三态**：请求反序列化不要配置 `Nulls.SKIP`（`JsonSetter(nulls=…)`）、`@JsonInclude(NON_NULL)` 等全局策略；不要做全局 `""→null`；不要引入全局 trim（`StringTrimmerEditor`） | paragon 无法区分"缺省"与"显式 null"，整份更新契约失效 |
| 2 | **反序列化必须走应用侧 `ObjectMapper`**：不要注册自己的 `ObjectMapper` / `MappingJackson2HttpMessageConverter` 去遮掉 Spring Boot 的构建链；需要额外 Jackson 配置时**也用 `Jackson2ObjectMapperBuilderCustomizer`（叠加式）** | paragon 的 `cn.labzen.paragon.common.json.CoreEnhanceJacksonCustomizer`（`JavaTimeModule` + `Jdk8Module` + SmartLong/BigInteger/BigDecimal 序列化器，以及契约待落地的 `providedFields` 修饰器）会静默失效；`LocalDateTime` 等类型直接绑不上 |
| 3 | **缺省字段不得被自动当成校验失败**：JSON 里没出现的字段，除业务显式声明的校验注解（`@NotNull` 等）外**不许**自动 400 | paragon "改了才动"的语义被破坏 |
| 4 | **不得改动既有响应契约**：`Result`（接口，仅 `code()` / `message()` / `value()`，实现类 `ValueResult` / `FileResult` 为 record）、`Results`、统一错误信封、`RequestException` 的语义与形态保持不变 | paragon 前端契约与回归测试大面积失效 |

---

## 3. 🟡 边界（现有行为必须逐字保持不变）

| 项 | 要求 |
| --- | --- |
| GET / 查询 | 继续 query / `@ModelAttribute` 绑定；**不要**为 GET 支持 body |
| form 提交 | `application/x-www-form-urlencoded` 的绑定行为与今天**完全一致**（paragon 全部控制器都靠它，必须支持灰度迁移） |
| multipart | `FileController#imports`（`@RequestParam MultipartFile`）保持现状 |
| 集合参数 | `@RequestParam("ids") List<ID>` 的逗号分隔语义保持现状（模板方法 `removes` 依赖） |
| 既有解析器 | `PageableCompatibleArgumentResolver` 等行为不变 |
| 元数据 | 新增/改写的绑定注解若需 `MetadataGenerateProcessor`、`LabzenWebConfigurer`、`LoggableControllerMetaRegistry` 感知，请一并同步 |
| 参数名 | 生成代码须保留方法参数名（`-parameters`）；`EvaluateMethodsProcessor:134-144` 的 `arg0/arg1` 告警请保留 |

---

## 4. 关键路径提醒（最容易白做的一点）

**若改动只落在运行时，paragon 的 `*ControllerImpl` 不会变 ⇒ 等于没生效。**
新绑定方式必须能在**接口注解层**表达，并让 `CreativeProcessor`（JavaPoet）把它写进生成的 Impl；`Suggestion` 机制是现成挂点。

---

## 5. 设计岔路口（已定：方案 A）

| 方案 | 说明 | 评价 |
| --- | --- | --- |
| **A（已采用）新注解 `@ResourceBody`** | 显式、无歧义、易测；配置决定落 `@ModelAttribute` 还是 `@RequestBody` | ✅ 语义最干净（消费方已确认可用） |
| B. 同一注解按 `Content-Type` 双语义 | 消费方零改动、可灰度；但一个端点两条绑定路径，需定优先级并防 content-type 混淆 | ⚠️ 未采用 |
| C. A + 该注解同时接受 form/json | 注解显式，同一注解下两种 `Content-Type` 均可绑 | ⚠️ 未采用：**同注解双语义会让"前端实际发的是哪种 Content-Type"变成隐式依赖**，消费方倾向显式切换 |

> 补充（消费方的实际用法）：**编译期全局开关 + 端点级显式注解覆盖**已满足"逐端点灰度"（显式写 `@ModelAttribute` 的端点不会翻转），因此不需要 B / C。

---

## 6. 需要 web 侧自行定夺并文档化的策略（不必回问）

| 情形 | 建议 |
| --- | --- |
| 报文含未声明的键（typo） | 写出明确策略；建议可配置 `FAIL_ON_UNKNOWN_PROPERTIES`（在"缺省即跳过"模型下，静默忽略 typo 极难排查） |
| primitive 字段收到显式 `null`（`{"frozen":null}`） | 请落成**可配置的兜底开关**（见 §10.1） |
| 空字符串 `""` | 原样传递，**不要**转 `null`（由消费方自行决定语义） |
| JSON 语法错误 / 类型不匹配 | 与 `@Validated` 失败**同一信封** + 400 |

---

## 7. 验收清单（可执行）

- [ ] `Content-Type: application/json` + `{"x": null}` ⇒ 控制器收到 `x == null`，且**可区分于** `{}`（消费方靠 `providedFields` 判定）
- [ ] `{}` ⇒ 控制器收到"字段缺省"的信号（而非"显式 null"）
- [ ] `application/x-www-form-urlencoded` 请求行为与改动前**逐字节一致**
- [ ] `@Validated` 失败 / JSON 语法错误 / 类型不匹配 ⇒ 统一信封 + 400，字段级信息与今天同形
- [ ] `LocalDateTime`、大整数等类型的反序列化行为与 `CoreEnhanceJacksonCustomizer` 预期一致（证明确实走了应用的 `ObjectMapper`）
- [ ] GET / multipart / `@RequestParam List` 三条路径回归测试通过
- [ ] 生成的 Impl 保留参数名（无 `argN` 告警）
- [ ] 文档产出：`Content-Type × 注解 × 绑定来源` 绑定矩阵 + "框架保证 / 不保证什么"（明确写明**不做任何空值归一**）

---

## 8. paragon 侧现状（供估算回归成本）

- 约 35 个域控制器，**全部经 `web-processor` 生成 Impl**；业务域 Realm 在 Controller 之下。
- **审计（`@Audited`）与 web 无关**，勿担心：`AuditAspect` 的切点是 `@Around("@annotation(audited)")`，`@Audited` 为 `@Target(METHOD)` 且约定只标注在业务域 Realm 入口方法上（切面还会校验 Bean 包路径）；参数名取自 **Realm 方法**（`AuditMetadataRegistry`），不经过生成的 Controller Impl。
- 过滤器链（`TenantResolverFilter` / `SubjectResolverFilter` / `LocaleResolvingFilter`）只读请求头与路径，不受绑定方式影响。
- 消费方的 Jackson 定制只有一个：`common/src/main/java/cn/labzen/paragon/common/json/CoreEnhanceJacksonCustomizer`（见红线 2）。

---

## 9. 建议的最小可交付切片

1. 一个接口方法 + 一个含可空字段的 DTO；
2. 三条测试：`{}` / `{"x":null}` / `{"x":""}` 在控制器侧**可区分**；
3. form 与 JSON 两条路径都通；
4. 先只覆盖这一个切片，其余端点不动 —— 让消费方灰度验证后再推广。

---

## 10. 消费方反馈与待办（2026-10-01）

> 以下三项是消费方（paragon）评估后的**明确诉求**，请 web 侧定夺并实现。前两项涉及运行期行为，第三项涉及文档口径。

### 10.1 基本类型 + 显式 `null`：请提供兜底开关

**问题**：JSON 下 `{"x": null}` 落到 `private boolean x` 时，Jackson 会把它变成**类型默认值**（`false`），"显式 null"这一态**在反序列化阶段就消失了**。消费方用"报文里出现过哪些字段"（`providedFields`）区分缺省与显式 null，因此会判定为"客户端要求 `false`"并**写入默认值** ⇒ 形成静默误写（例如静默撤销授权）。

**消费方侧约定（契约要求）**：请求侧 DTO 的字段**一律使用包装类型**（`Boolean` / `Long` / `Integer`…），使三态在类型上天然可表达。

**请 web 侧提供（兜底）**：一个**配置项**，开启时对**请求反序列化**启用 Jackson 的 `FAIL_ON_NULL_FOR_PRIMITIVES`：

| 项 | 建议 |
| --- | --- |
| 配置项 | 例如 `web.request.fail-on-null-for-primitives`（命名由你定） |
| 默认值 | **`false`**（不改变既有消费方行为） |
| 生效点 | 请求 `@RequestBody` 的反序列化（Jackson deserialization feature；不影响序列化） |
| 命中表现 | `MismatchedInputException`（被包为 `HttpMessageNotReadableException`）⇒ 由 `LabzenHandlerExceptionResolver` 输出 **`code=400`**；message 中含 Jackson 的 `through reference chain: Xxx["field"]`（字段路径可读，但**不是** `data.validator` 结构） |
| 目的 | 把"开发者在 DTO 里写了 primitive"这一失误，从**静默误写**变成**响亮 400** |

### 10.2 空请求体 / 字面量 `null` body：请给出明确保障

**现状**：JSON 模式下 `@RequestBody` 的请求若**没有 body**（或 body 是字面量 `null`），会抛 `HttpMessageNotReadableException` ⇒ `code=400`；而 FORM 模式下"没有字段"等价于"全部缺省"、请求**正常受理**。⇒ **同一接口在两种模式下行为不一致**。

**请 web 侧二选一（并写入文档与测试）**：

- **方案 A（最低要求）**：**文档化**这条差异 —— JSON 模式下前端**必须至少发送 `{}`**；空 body ⇒ `code=400`。
- **方案 B（行为对齐，即消费方要的"保障"）**：提供编译期配置（如 `processor.resource-body.empty=REJECT|ALLOW`），`ALLOW` 时对 `@ResourceBody` 参数生成 `@RequestBody(required = false)`，并在空 body 时用一个**空实例**兜底：

  ```java
  // 生成结果（示意）
  Result create(@Validated @RequestBody(required = false) UserDto resource) {
    return userRealm.create(resource == null ? new UserDto() : resource);
  }
  ```

  ⇒ 使 JSON 模式与 FORM 模式语义一致（"空" = 全部缺省），消费方前端无需为两种模式写两套调用。

  - 可行性：`RB` 的**具体类型在生成期已知**（泛型已解析为 `PermissionDto` 之类），可安全生成 `new RB()`；消费方 DTO 普遍具备无参构造。
  - 代价：生成代码多一个三元表达式；"空 body" 与 "`{}`" 需在文档里写成同一语义。
  - 若采纳，请写清：**空 body ⇒ 等价于 `{}`**。

> 消费方立场：**A 是底线、B 才是保障**。若 B 因故不做，请给出明确回执 —— 消费方会按 A 在自身文档与前端侧兜住（要求前端一律发 `{}`）。

### 10.3 HTTP 状态码：请保持"默认 200 + 信封 `code`"，并补进文档

**现状（消费方已实测确认）**：后端默认**始终返回 HTTP 200**，真实状态在响应信封的 `code` 字段（`LabzenHandlerExceptionResolver` 不设置 HTTP 状态；全仓仅 `LabzenRestRequestHandlerInterceptor` 的 406 分支会设置 HTTP 状态）。

**请 web 侧**：

1. 在框架文档中**明确写出**这一口径（目前只在 `resource-body-binding.md` §6 里提了一句"不修改 HTTP 响应状态码"）；
2. 后续若提供"返回真实 HTTP 状态"的配置，**默认保持"否"** —— 消费方前端目前按信封 `code` 判断，默认一旦改变会**全量失效**；
3. 若该配置可运行期切换，请在文档中提醒：切换前后前端判断逻辑必须同步调整。
