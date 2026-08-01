package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementClass;
import cn.labzen.web.apt.internal.element.ElementMethod;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.*;

/**
 * API 日志元数据生成处理器（优先级 7，在 CreativeProcessor 之后执行）。
 * <p>
 * 在编译期为每个标注了 {@code @LabzenController} 的接口生成元数据，
 * 包括 Controller 接口名、方法名、HTTP 方法、URL Pattern 等信息。
 * 生成的元数据将被注册到 {@code LoggableControllerMetaRegistryImplImpl} 类中，
 * 运行时通过 O(1) 的静态 Map 查表获取元数据，完全消除运行时反射开销。
 * <p>
 * <b>生成的元数据结构：</b>
 * <pre>{@code
 * // 每个 Controller 接口对应一个 ControllerMeta
 * public record ControllerMeta(
 *     String interfaceName,          // 接口全限定名，如 "com.example.controller.UserController"
 *     String simpleName,             // 接口简单名，如 "UserController"
 *     Map<String, MethodMeta> methods  // 方法标识 → 方法元数据
 * ) {}
 *
 * // 每个方法对应一个 ControllerMethodMeta
 * public record ControllerMethodMeta(
 *     String methodName,             // 方法名，如 "create"
 *     String httpMethod,             // HTTP 方法，如 "POST"
 *     String urlPattern,             // URL 模式，如 "/api/user"
 *     String fullUrlPattern,         // 完整 URL 模式（含类级别路径），如 "/api/user"
 *     List<String> parameterTypes    // 参数类型的简单名称列表
 * ) {}
 * }</pre>
 * <p>
 * <b>执行时机：</b>在 {@link CreativeProcessor}（优先级 6）之后执行，
 * 此时 Controller 实现类已经生成完毕，ElementClass 包含完整的方法和注解信息。
 *
 * @see cn.labzen.web.api.log.registry.LoggableControllerMetaRegistry
 */
public final class MetadataGenerateProcessor implements InternalProcessor {

  /**
   * 元数据注册表实现类的包名
   */
  private static final String REGISTRY_PACKAGE = "cn.labzen.web.log.generated";

  /**
   * 元数据注册表接口
   */
  private static final String REGISTRY_INTERFACE_NAME = "LoggableControllerMetaRegistry";

  /**
   * 元数据注册表实现类的简单名
   */
  private static final String REGISTRY_CLASS_NAME = "LoggableControllerMetaRegistryImpl";

  /**
   * 已处理的 Controller 接口名集合（避免重复生成）
   */
  private final Set<String> processedControllers = new HashSet<>();

  /**
   * 生成当前 Controller 的元数据并写入注册表。
   * <p>
   * 由于 APT 处理器是每个 Controller 独立调用的，无法在一次 process 中聚合所有 Controller。
   * 因此采用"追加写入"策略：每次 process 时读取现有注册表内容，追加新的 Controller 元数据，
   * 再整体写回。
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    ElementClass root = context.getRoot();
    // 从源 TypeElement 获取接口名（root.getName() 是生成的实现类名，如 UserControllerImpl）
    String controllerSimpleName = context.getSource().getSimpleName().toString();
    String interfaceName = root.getPkg() + "." + controllerSimpleName;

    // 跳过已处理的 Controller（避免重复）
    if (!processedControllers.add(interfaceName)) {
      return;
    }

    AnnotationProcessorContext apc = context.getApc();
    Filer filer = apc.filer();

    // 解析类级别的 @RequestMapping（获取 URL 前缀）
    String classLevelPath = extractClassLevelPath(root);

    // 构建方法元数据 Map
    Map<String, ControllerMethodMeta> methodsMeta = new LinkedHashMap<>();
    for (ElementMethod method : root.getMethods()) {
      String methodName = method.getName();
      String httpMethod = extractHttpMethod(method);
      String urlPattern = extractUrlPattern(method);
      String fullUrlPattern = buildFullUrl(classLevelPath, urlPattern);

      List<String> parameterTypes = method.getParameters().stream()
        .map(p -> Utils.getSimpleName(p.getType()))
        .toList();

      // 构建两个 key：方法名 和 HTTP方法+URL
      String methodNameKey = methodName;
      String httpUrlKey = httpMethod + " " + fullUrlPattern;

      ControllerMethodMeta meta = new ControllerMethodMeta(methodName, httpMethod, urlPattern, fullUrlPattern, parameterTypes);

      methodsMeta.put(methodName, meta);
      // 避免重复（如 find 方法的 GET / 可能与 info 的 GET /{id} 冲突时不覆盖）
      if (!methodsMeta.containsKey(httpUrlKey)) {
        methodsMeta.put(httpUrlKey, meta);
      }
    }

    // 生成注册表 Java 源代码
    String sourceCode = generateRegistrySource(controllerSimpleName, interfaceName, methodsMeta);

    // 输出源文件
    try {
      String qualifiedName = REGISTRY_PACKAGE + "." + REGISTRY_CLASS_NAME;
      JavaFileObject sourceFile = filer.createSourceFile(qualifiedName, context.getSource());
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(sourceCode);
      }
    } catch (Exception e) {
      apc.messaging().warning("MetadataGenerateProcessor: 无法生成元数据注册表: " + e.getMessage());
    }
  }

  /**
   * 提取类级别的 @RequestMapping 路径前缀。
   *
   * @param root 元素类
   * @return 路径前缀（如 "/api/user"），无则返回 ""
   */
  private String extractClassLevelPath(ElementClass root) {
    for (ElementAnnotation annotation : root.getAnnotations()) {
      if (isRequestMappingAnnotation(annotation)) {
        Object value = annotation.getMembers().get("value");
        if (value instanceof List<?> list && !list.isEmpty()) {
          return String.valueOf(list.getFirst());
        }
        if (value instanceof String s && !s.isEmpty()) {
          return s;
        }
      }
    }
    return "";
  }

  /**
   * 判断是否为 Spring RequestMapping 相关注解。
   */
  private boolean isRequestMappingAnnotation(ElementAnnotation annotation) {
    TypeName type = annotation.getType();
    return type instanceof ClassName cn && Utils.isRequestMappingAnnotation(cn);
//    String simpleName = Utils.getSimpleName(type);
//    return simpleName.endsWith("Mapping");
  }

  /**
   * 从方法注解中提取 HTTP 方法。
   * <p>
   * 支持的注解：{@code @GetMapping → "GET"}、{@code @PostMapping → "POST"}、
   * {@code @PutMapping → "PUT"}、{@code @DeleteMapping → "DELETE"}、
   * {@code @PatchMapping → "PATCH"}、{@code @RequestMapping → 从 method 属性提取}
   *
   * @param method 方法元素
   * @return HTTP 方法字符串，默认为 "GET"
   */
  private String extractHttpMethod(ElementMethod method) {
    for (ElementAnnotation annotation : method.getAnnotations()) {
      String simpleName = Utils.getSimpleName(annotation.getType());
      if (simpleName.endsWith("Mapping")) {
        // 直接映射注解名到 HTTP 方法
        return switch (simpleName) {
          case "PostMapping" -> "POST";
          case "PutMapping" -> "PUT";
          case "DeleteMapping" -> "DELETE";
          case "PatchMapping" -> "PATCH";
          case "RequestMapping" -> {
            // @RequestMapping 需从 method 属性提取
            Object methodValue = annotation.getMembers().get("method");
            if (methodValue instanceof List<?> list && !list.isEmpty()) {
              yield list.getFirst().toString();
            }
            yield "GET"; // 默认
          }
          default -> "GET";
        };
      }
    }
    return "GET";
  }

  /**
   * 从方法注解中提取 URL Pattern。
   *
   * @param method 方法元素
   * @return URL 模式字符串
   */
  private String extractUrlPattern(ElementMethod method) {
    for (ElementAnnotation annotation : method.getAnnotations()) {
      TypeName type = annotation.getType();
      if (type instanceof ClassName cn && Utils.isRequestMappingAnnotation(cn)) {
        Object value = annotation.getMembers().get("value");
        if (value instanceof List<?> list && !list.isEmpty()) {
          return String.valueOf(list.getFirst());
        }
        if (value instanceof String s) {
          return s;
        }
      }
    }
    return "";
  }

  /**
   * 构建完整的 URL 路径。
   *
   * @param classPath  类级别路径（如 "/api/user"）
   * @param methodPath 方法级别路径（如 "/{id}"）
   * @return 完整路径（如 "/api/user/{id}"）
   */
  private String buildFullUrl(String classPath, String methodPath) {
    StringBuilder sb = new StringBuilder();
    if (classPath != null && !classPath.isEmpty()) {
      sb.append(classPath.startsWith("/") ? classPath : "/" + classPath);
    }
    if (methodPath != null && !methodPath.isEmpty()) {
      if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '/' && methodPath.startsWith("/")) {
        sb.append(methodPath.substring(1));
      } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '/' && !methodPath.startsWith("/")) {
        sb.append("/").append(methodPath);
      } else {
        sb.append(methodPath);
      }
    }
    return sb.toString();
  }

  /**
   * 生成完整的注册表实现类源代码。
   * <p>
   * 生成的类实现 {@code cn.labzen.web.api.log.ApiLogControllerRegistry} 接口，
   * 使用静态 Map 存储所有 Controller 元数据。
   *
   * @param controllerSimpleName Controller 简单名
   * @param interfaceName        接口全限定名
   * @param methodsMeta          方法元数据
   * @return Java 源代码字符串
   */
  private String generateRegistrySource(String controllerSimpleName, String interfaceName, Map<String, ControllerMethodMeta> methodsMeta) {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(REGISTRY_PACKAGE).append(";\n\n");

    // imports
    sb.append("import cn.labzen.web.api.log.ApiLogControllerRegistry;\n");
    sb.append("import cn.labzen.web.api.log.registry.ControllerMeta;\n");
    sb.append("import cn.labzen.web.api.log.registry.ControllerMethodMeta;\n");
    sb.append("import java.util.*;\n\n");

    // 类定义
    sb.append("/**\n");
    sb.append(" * API 日志 Controller 元数据注册表（编译期自动生成）。\n");
    sb.append(" * <p>\n");
    sb.append(" * 由 MetadataGenerateProcessor 在编译期生成，提供 O(1) 静态 Map 查表。\n");
    sb.append(" * 禁止手动编辑此文件。\n");
    sb.append(" */\n");
    sb.append("@SuppressWarnings(\"all\")\n");
    sb.append("public final class ").append(REGISTRY_CLASS_NAME).append(" implements ").append(REGISTRY_INTERFACE_NAME).append(" {\n\n");

    // 静态注册表 Map
    sb.append("  private static final Map<String, ControllerMeta> REGISTRY = new LinkedHashMap<>();\n\n");

    // 静态初始化块
    sb.append("  static {\n");
    sb.append("    // ").append(controllerSimpleName).append("\n");
    sb.append("    {\n");
    sb.append("      Map<String, MethodMeta> methods = new LinkedHashMap<>();\n");

    for (Map.Entry<String, ControllerMethodMeta> entry : methodsMeta.entrySet()) {
      ControllerMethodMeta meta = entry.getValue();
      sb.append("      methods.put(\"").append(escapeJava(entry.getKey())).append("\",\n");
      sb.append("        new MethodMeta(\n");
      sb.append("          \"").append(escapeJava(meta.methodName)).append("\",\n");
      sb.append("          \"").append(escapeJava(meta.httpMethod)).append("\",\n");
      sb.append("          \"").append(escapeJava(meta.urlPattern)).append("\",\n");
      sb.append("          \"").append(escapeJava(meta.fullUrlPattern)).append("\",\n");
      sb.append("          ").append(generateListLiteral(meta.parameterTypes)).append("\n");
      sb.append("        ));\n");
    }

    sb.append("      REGISTRY.put(\"").append(escapeJava(controllerSimpleName)).append("\", new ControllerMeta(\"");
    sb.append(escapeJava(interfaceName)).append("\", \"");
    sb.append(escapeJava(controllerSimpleName)).append("\", Collections.unmodifiableMap(methods)));\n");
    sb.append("    }\n");
    sb.append("  }\n\n");

    // 实现接口方法
    sb.append("  @Override\n");
    sb.append("  public Map<String, ControllerMeta> getAllMetas() {\n");
    sb.append("    return Collections.unmodifiableMap(REGISTRY);\n");
    sb.append("  }\n\n");

    sb.append("  @Override\n");
    sb.append("  public Optional<ControllerMeta> lookup(String controllerSimpleName) {\n");
    sb.append("    return Optional.ofNullable(REGISTRY.get(controllerSimpleName));\n");
    sb.append("  }\n\n");

    sb.append("  @Override\n");
    sb.append("  public Optional<MethodMeta> lookupMethod(String controllerSimpleName, String methodKey) {\n");
    sb.append("    return lookup(controllerSimpleName)\n");
    sb.append("      .flatMap(meta -> Optional.ofNullable(meta.methods().get(methodKey)));\n");
    sb.append("  }\n");
    sb.append("}\n");

    return sb.toString();
  }

  /**
   * 生成 List 字面量（如 {@code Arrays.asList("param1", "param2")}）。
   */
  private String generateListLiteral(List<String> items) {
    if (items.isEmpty()) {
      return "List.of()";
    }
    StringBuilder sb = new StringBuilder("Arrays.asList(");
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("\"").append(escapeJava(items.get(i))).append("\"");
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * 转义 Java 字符串中的特殊字符。
   */
  private String escapeJava(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t");
  }

  /**
   * 方法元数据内部记录。
   */
  private record ControllerMethodMeta(
    String methodName,
    String httpMethod,
    String urlPattern,
    String fullUrlPattern,
    List<String> parameterTypes
  ) {
  }

  @Override
  public int priority() {
    return PRIORITY_METADATA;
  }
}
