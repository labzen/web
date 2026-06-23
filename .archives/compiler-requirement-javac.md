# web-processor 必须使用 javac 编译器的原因

## 问题表现

使用 web-processor 的项目，在 IDEA 中 `Build → Build Project` 或使用 Eclipse JDT 编译器（ECJ）编译时，
编译过程不报错，但启动项目后某些类的方法体中包含：

```java
throw new Error("Unresolved compilation problem: \n\t... cannot be resolved\n");
```

`mvn clean compile` 后检查 `.class` 文件，会发现大量类受到污染。

## 根因分析

### 1. ECJ 与 javac 的编译轮次差异

JSR 269（注解处理 API）只规定了处理器通过 `Filer.createSourceFile()` 生成文件，
但**未规定生成的文件何时被编译**。不同编译器实现不同：

| 编译器 | 行为 |
|--------|------|
| **javac** | 保守：生成的文件排队，**下一轮**才编译 |
| **ECJ / IntelliJ 编译器** | 激进：生成的文件**同轮**内立即尝试编译 |

javac 的多轮次机制天然安全——所有依赖类型在前几轮已完成编译，
下一轮编译生成的文件时可以正确解析所有引用。

ECJ 的同轮编译在大多数场景下更高效，但当 web-processor 生成的代码引用了
**尚未在本轮完成编译的类型**时，就会出现灾难性的级联 stub。

### 2. web-processor 的独特处境

web-processor 生成的 Controller 实现类会委托调用业务 Service 层的方法，
而 Service 层的方法参数通常是项目中的领域对象（DTO、Entity 等）。

这些领域对象往往依赖**其他注解处理器**（如 Lombok、MapStruct 等）进行增强——
例如通过 Lombok 生成 `getter`/`setter`/`equals`/`hashCode`/`toString`，
或通过其他处理器生成辅助代码。

这意味着 web-processor 生成代码的**间接依赖链**比大多数注解处理器更深：

```
生成的 ControllerImpl
  → 调用 Service 方法
    → 参数是 DTO/Entity
      → DTO/Entity 依赖 Lombok 等处理器的产物
        → DTO/Entity 继承基类（如 MyBatis-Flex 的 BaseModel）
```

在 ECJ 的同轮编译中，这些间接依赖的类型可能尚未完成**所有注解处理器的处理**，
导致方法（如 Lombok 生成的 `toString()`）无法解析 → 产生 stub。

### 3. 为什么其他 APT 通常不要求 javac

大多数注解处理器生成的代码具有**浅层依赖**：

- **不生成新文件的处理器**（如 Lombok）：直接修改已有类的 AST，不产生跨文件依赖
- **生成 Mapper/Table 辅助类的处理器**（如 MapStruct、MyBatis-Flex）：生成的代码只引用简单 POJO，
  不涉及深层继承链

web-processor 的生成的代码需要"穿过 Service 层 → 到达业务 DTO → 再经过其他处理器的增强"，
这条依赖链路是它独有的特征，也是它与 ECJ 不兼容的根本原因。

### 4. ECJ 的 proceedOnError 机制

ECJ 和 IntelliJ 编译器在默认配置下，遇到编译错误不会中止整个编译，
而是将错误类的方法体替换为 `throw new Error("Unresolved compilation problem")` 的占位字节码（stub）。
这会导致：

```
某个底层类编译出错 → 变 stub
  → 依赖它的类编译时看到 stub → 级联变 stub
  → 大量类被污染
```

一个问题源头的 stub 会级联污染整个编译产物。

## 解决方案

### Maven 命令行编译

在项目 `parent/pom.xml` 的 `maven-compiler-plugin` 配置中加入：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <forceLegacyJavacApi>true</forceLegacyJavacApi>
        <compilerId>javac</compilerId>
        <!-- 其他配置保持不变 -->
        <parameters>true</parameters>
        ...
    </configuration>
</plugin>
```

- `forceLegacyJavacApi`：强制使用 javax.tools.JavaCompiler API（javac）
- `compilerId`：指定编译器实现为 javac

### IntelliJ IDEA 配置

`Settings → Build, Execution, Deployment → Compiler → Java Compiler`

- **Use compiler**: 选择 `Javac`（而非 `Eclipse` 或默认选项）

### Eclipse IDE 配置

由于 web-processor 与 ECJ 不兼容，建议使用 Eclipse 的项目转换为 Maven 运行方式：
- `Run → Run Configurations → Maven Build`，执行 `compile` 目标

## 注意事项

1. **不影响命令行编译**：`mvn compile` 默认使用 javac，无需额外配置。若看到 ECJ 相关配置，
   请确保已按上述方案覆盖。

2. **CI/CD 环境**：根据 CI 环境使用的 JDK 不同，可能已有默认编译器设置，建议显式配置以确保一致性。

3. **不影响运行时**：此配置仅影响编译阶段，对项目运行时行为无任何影响。

4. **新版本 JDK 兼容性**：JDK 23+ 可能移除旧版 javac API，届时 `forceLegacyJavacApi`
   可能需要调整。建议跟踪 Maven Compiler Plugin 的最新文档。

## 历史记录

- 2026-06-23：确认根因（ECJ 同轮编译 + proceedOnError 级联 stub），采用 javac 方案
- 2026-06-22：初步怀疑 processor 状态污染（已修复 `processedControllers`、`parsedMethods` 等清理问题）
- 2026-06-16：首次发现 `Unresolved compilation problem` 问题
