# 资源入参绑定（@ResourceBody）

## 1. 作用

`@ResourceBody` 是标注在 Controller 接口方法资源参数上的注解，用于把「资源入参是用表单还是 JSON 传递」这一决策
交给配置，而不需要改动接口代码。

同一个接口，只需在 `labzen.web.config` 中切换一个配置项，生成的 Controller 实现类就会改用另一种 Spring 绑定注解：

- `processor.resource-binding=FORM`（默认）→ 生成为 `@ModelAttribute`，入参取自 `application/x-www-form-urlencoded` 或 query
- `processor.resource-binding=JSON` → 生成为 `@RequestBody`，入参取自 `application/json`

不配置该项时，行为与未引入本注解时一致。

## 2. 机制

绑定方式必须在**接口注解层**表达：`web-processor` 在生成 `*ControllerImpl` 时，会把接口方法参数的注解写入实现类。
`@ResourceBody` 参与该过程，在**编译期**被替换为 Spring 原生的 `@ModelAttribute` 或 `@RequestBody`，因此：

- 运行时不需要任何额外组件（没有自定义 `HandlerMethodArgumentResolver`、没有自定义 `ObjectMapper` 或 `HttpMessageConverter`）；
- JSON 绑定直接复用 Spring 原生机制，使用应用侧配置的 `ObjectMapper`，`LocalDateTime`、大整数等类型以及应用侧对 Jackson 的定制均照常生效。

`@ResourceBody` 的保留策略为 `RetentionPolicy.CLASS`。框架提供的基础接口（如 `StandardController`）以 class 形式被应用继承，
`CLASS` 保留策略保证注解处理器在读取「继承自 class 的接口方法」时仍能获取到该注解；它不会出现在生成的实现类中。

## 3. 配置

### 3.1 编译期配置（`labzen.web.config`）

文件放置在模块根目录或项目根目录均可，项目根配置用于补充模块未设置的项。

| 配置项 | 可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `processor.resource-binding` | `FORM` / `JSON` | `FORM` | `@ResourceBody` 在生成的实现类中的绑定方式 |
| `processor.resource-body.empty` | `REJECT` / `ALLOW` | `REJECT` | JSON 绑定下遇到空请求体的处理方式：`REJECT` 按 400 处理；`ALLOW` 生成 `@RequestBody(required = false)` 并以空实例兜底 |

> 这两项属于 APT（编译期）配置，运行时不需要感知。

### 3.2 运行时配置（`labzen.yml`）

| 配置项 | 可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `web.request.fail-on-null-for-primitives` | `true` / `false` | `false` | 开启后，请求反序列化时基本类型字段收到显式 `null` 报错（400），避免被静默赋予类型默认值 |

> 该项通过叠加式 Jackson 定制生效，不替换应用侧的 `ObjectMapper`，也不影响序列化。

## 4. 绑定矩阵

| 接口参数标注 | FORM（默认）生成 | JSON 生成 | 实际入参来源 |
| --- | --- | --- | --- |
| `@ResourceBody RB resource` | `@ModelAttribute RB resource` | `@RequestBody RB resource` | 表单/query 或 JSON body |
| `@ResourceBody` + 任一显式 Spring 绑定注解 | 保留显式注解 | 保留显式注解 | 由显式注解决定 |
| `@ModelAttribute RB resource` | `@ModelAttribute` | `@ModelAttribute` | 表单/query |
| `@RequestBody RB resource` | `@RequestBody` | `@RequestBody` | JSON body |
| `@RequestParam` / `@PathVariable` | 原样 | 原样 | query / path |
| `@ResourceBody` 出现在 GET 入口参数 | `@ModelAttribute`（编译告警） | `@ModelAttribute`（编译告警） | query |

`Content-Type` 与绑定来源的对应关系（表单模式）：

| 请求 | 说明 |
| --- | --- |
| `POST/PUT` + `application/x-www-form-urlencoded` | 绑定资源参数 |
| `GET` + query string | 绑定资源参数（`find` 等） |
| `GET` + `@RequestBody` | 不适用，GET 无请求体 |
| `multipart/form-data` | 由 `FileController#imports` 的 `@RequestParam MultipartFile` 处理 |

## 5. 落型规则

参数带有 `@ResourceBody` 时，处理器按以下顺序处理：

1. 若参数上已存在显式 Spring 绑定注解（`org.springframework.web.bind.annotation` 包下的任一注解），
   则移除 `@ResourceBody`、保留显式注解 —— 显式声明优先。
2. 若所在方法是 GET 请求入口（`@GetMapping` 或 `@RequestMapping(method = GET)`），不存在请求体，
   参数落为 `@ModelAttribute`，并输出编译期告警。
3. 其余情况按 `processor.resource-binding` 落为 `@ModelAttribute`（FORM）或 `@RequestBody`（JSON）。
4. 落为 `@RequestBody` 且 `processor.resource-body.empty=ALLOW` 时，生成 `@RequestBody(required = false)`，
   并在方法体中将 null 入参替换为该资源类型的空实例（`resource == null ? new XxxResource() : resource`）。

无论何种情况，`@ResourceBody` 都不会保留到生成的 Controller 实现类中。

关于 `ALLOW`：请求无请求体（或请求体为字面量 `null`）时 Spring 传入 `null`，方法体随即以空实例兜底，
与表单模式下「没有字段等价于全部缺省」的语义对齐。此路径下 Spring 不会对空实例执行字段校验
（发送 `{}` 时会执行），若资源类型存在必须的校验注解，请改用 `REJECT` 或保证前端发送 `{}`。

## 6. 框架保证与不保证

**保证**

- 不注册任何 `ObjectMapper` 或 `HttpMessageConverter`，不干扰应用侧对 Jackson 的定制。
- 不做任何空值归一：不配置 `Nulls.SKIP`、不配置 `@JsonInclude(NON_NULL)`、不做 `"" → null`、不引入 trim。
- FORM 模式下生成的实现类与未引入 `@ResourceBody` 时一致。
- 请求体无法解析（JSON 语法错误、类型不匹配）、表单绑定失败、请求体校验失败，均输出统一的标准响应信封，
  状态码写在信封的 `code` 字段。框架不修改 HTTP 响应状态码，真实状态码一律承载于信封的 `code` 字段
  （详见 [http-status-response-policy.md](http-status-response-policy.md)）。

**不保证**

- 空字符串 `""` 原样传递，不转换为 `null`，其语义由业务自行解释。
- 报文中未声明的字段：按 Spring Boot 默认行为静默忽略（不报错）。
- 基本类型字段收到显式 `null`（如 `{"frozen":null}`）：默认按 Spring Boot 默认行为取该类型的默认值，不报错；
  可通过 `web.request.fail-on-null-for-primitives=true` 改为报错（400）。

## 7. 示例

接口定义：

```java
@LabzenController
public interface UserController extends StandardController<UserRealm, UserDto, Long> {

  // 覆盖标准入口，入参使用 @ResourceBody
  @Override
  Result create(@Validated @ResourceBody UserDto resource);

  // 自定义入口同样使用 @ResourceBody
  @PostMapping("/recache/{id}")
  Result recache(@PathVariable Long id, @ResourceBody UserDto resource);

  // 若某一入口需要固定使用表单/query，直接显式声明（显式声明优先）
  @PostMapping("/search")
  Result search(@ModelAttribute UserQuery query);
}
```

`labzen.web.config`：

```
processor.resource-binding=JSON
```

生成结果（`target/generated-sources` 下的 `UserControllerImpl`）：

```java
public Result create(@Validated @RequestBody UserDto resource) { ... }
public Result recache(@PathVariable Long id, @RequestBody UserDto resource) { ... }
public Result search(@ModelAttribute UserQuery query) { ... }
```
