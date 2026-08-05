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
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * API 日志元数据生成处理器（优先级 7，在 CreativeProcessor 之后执行）。
 * <p>
 * 在编译期为每个标注了 {@code @LabzenController} 的接口生成一个 JSON 元数据文件，
 * 包含 Controller 接口名、方法名、HTTP 方法、URL Pattern 等信息。
 * <p>
 * 每个 Controller 生成一个独立的 JSON 文件，输出到
 * {@code META-INF/labzen/{ControllerName}.meta.json}，运行时由
 * {@code LoggableControllerMetaRegistry} 统一加载。
 * <p>
 * <b>JSON 文件格式：</b>
 * <pre>{@code
 * {
 *   "interfaceClass": "com.example.controller.UserController",
 *   "simpleName": "UserController",
 *   "methods": {
 *     "a1b2c3d4": {
 *       "methodName": "create",
 *       "httpMethod": "POST",
 *       "urlPattern": "",
 *       "fullUrlPattern": "/api/user",
 *       "parameterTypes": ["UserBean"]
 *     }
 *   }
 * }
 * }</pre>
 * <p>
 * methods 的 key 是方法签名（方法名+返回类型+参数类型列表）的 MD5 前 5 位（hex）。
 * 运行时注册表会自动组装 httpUrlKey（httpMethod + " " + fullUrlPattern）并注册三个 key。
 */
public final class MetadataGenerateProcessor implements InternalProcessor {

  /**
   * JSON 元数据文件输出目录（相对于 classpath 根）
   */
  private static final String META_OUTPUT_DIR = "META-INF/labzen";

  /**
   * 已处理的 Controller 接口名集合（避免重复生成）。
   * <p>
   * 注意：APT 处理器实例会在多轮编译中复用，此集合用于防止同一 Controller
   * 被多次处理产生重复的 JSON 文件。
   */
  private final Set<String> processedControllers = new HashSet<>();

  /**
   * 为当前 Controller 生成 JSON 元数据文件。
   */
  @Override
  public void process(ControllerContext context) {
    ElementClass root = context.getRoot();
    String controllerSimpleName = context.getSource().getSimpleName().toString();
    String interfaceName = root.getPkg() + "." + controllerSimpleName;

    if (!processedControllers.add(interfaceName)) {
      return;
    }

    AnnotationProcessorContext apc = context.getApc();
    Filer filer = apc.filer();

    // 解析类级别的 @RequestMapping（获取 URL 前缀）
    String classLevelPath = extractClassLevelPath(root);

    // 构建方法元数据 Map（key = 方法签名 MD5 前 5 位 hex）
    Map<String, Map<String, Object>> methodsMeta = new LinkedHashMap<>();
    for (ElementMethod method : root.getMethods()) {
      String methodName = method.getName();
      String httpMethod = extractHttpMethod(method);
      String urlPattern = extractUrlPattern(method);
      String fullUrlPattern = buildFullUrl(classLevelPath, urlPattern);

      String returnType = Utils.getSimpleName(method.getReturnType());
      List<String> parameterTypes = method.getParameters().stream()
        .map(p -> Utils.getSimpleName(p.getType()))
        .toList();
      String hash = hashControllerMethod(methodName, returnType, parameterTypes);

      Map<String, Object> meta = new LinkedHashMap<>();
      meta.put("methodName", methodName);
      meta.put("httpMethod", httpMethod);
      meta.put("urlPattern", urlPattern);
      meta.put("fullUrlPattern", fullUrlPattern);
      meta.put("parameterTypes", parameterTypes);

      methodsMeta.putIfAbsent(hash, meta);
    }

    // 构建顶层 JSON
    Map<String, Object> controllerMeta = new LinkedHashMap<>();
    controllerMeta.put("interfaceName", interfaceName);
    controllerMeta.put("simpleName", controllerSimpleName);
    controllerMeta.put("methods", methodsMeta);

    // 输出 JSON 文件
    try {
      String resourcePath = META_OUTPUT_DIR + "/" + controllerSimpleName + ".meta.json";
      FileObject fileObject = filer.createResource(
        StandardLocation.CLASS_OUTPUT, "", resourcePath, context.getSource());
      try (Writer writer = fileObject.openWriter()) {
        writer.write(toJson(controllerMeta));
      }
    } catch (Exception e) {
      apc.messaging().warning("MetadataGenerateProcessor: 无法生成元数据 JSON 文件: " + e.getMessage());
    }
  }

  // ============================================================
  // 注解解析
  // ============================================================

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

  private boolean isRequestMappingAnnotation(ElementAnnotation annotation) {
    TypeName type = annotation.getType();
    return type instanceof ClassName cn && Utils.isRequestMappingAnnotation(cn);
  }

  private String extractHttpMethod(ElementMethod method) {
    for (ElementAnnotation annotation : method.getAnnotations()) {
      String simpleName = Utils.getSimpleName(annotation.getType());
      if (simpleName.endsWith("Mapping")) {
        return switch (simpleName) {
          case "PostMapping" -> "POST";
          case "PutMapping" -> "PUT";
          case "DeleteMapping" -> "DELETE";
          case "PatchMapping" -> "PATCH";
          case "RequestMapping" -> {
            Object methodValue = annotation.getMembers().get("method");
            if (methodValue instanceof List<?> list && !list.isEmpty()) {
              yield list.getFirst().toString();
            }
            yield "GET";
          }
          default -> "GET";
        };
      }
    }
    return "GET";
  }

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

  // ============================================================
  // 简易 JSON 序列化（不引入第三方依赖）
  // ============================================================

  /**
   * 将 Map 结构序列化为 JSON 字符串。
   * <p>
   * 使用手动拼接方式，避免在 APT 处理器中引入 Jackson/Gson 等依赖。
   */
  private static String toJson(Object obj) {
    if (obj == null) return "null";
    if (obj instanceof String s) return "\"" + escapeJson(s) + "\"";
    if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
    if (obj instanceof Map<?, ?> map) {
      StringBuilder sb = new StringBuilder("{\n");
      int i = 0;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (i > 0) sb.append(",\n");
        sb.append("  \"").append(entry.getKey()).append("\": ");
        sb.append(toJson(entry.getValue()));
        i++;
      }
      sb.append("\n}");
      return sb.toString();
    }
    if (obj instanceof List<?> list) {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(toJson(list.get(i)));
      }
      sb.append("]");
      return sb.toString();
    }
    return "\"" + escapeJson(obj.toString()) + "\"";
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t");
  }

  /**
   * 计算输入字符串的 MD5 哈希并返回前 5 位十六进制字符串。
   */
  private String hashControllerMethod(String methodName, String returnType, List<String> parameterTypes) {
    String parameters = String.join(", ", parameterTypes);
    String signature = String.format("%s %s(%s)", returnType, methodName, parameters);
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      byte[] bytes = signature.getBytes();
      byte[] digest = md5.digest(bytes);
      String hex = HexFormat.of().formatHex(digest);
      return hex.substring(0, 5);
    } catch (NoSuchAlgorithmException e) {
      // MD5 是 Java 标准算法，不会发生此异常
      return Integer.toHexString(signature.hashCode());
    }
  }

  @Override
  public int priority() {
    return PRIORITY_METADATA;
  }
}
