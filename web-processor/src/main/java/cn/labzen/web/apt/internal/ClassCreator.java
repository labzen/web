package cn.labzen.web.apt.internal;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.LabzenWebProcessor;
import cn.labzen.web.apt.internal.element.*;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.labzen.web.apt.definition.JUnitConstants.JUNIT_OUTPUT_DIR;

/**
 * JavaPoet 代码生成器
 * <p>
 * 负责将内存中的 ElementClass 结构转换为 Java 源代码文件。
 * 主要功能：
 * <ul>
 *   <li>加载方法体模板文件</li>
 *   <li>构建类结构（类名、父接口、注解）</li>
 *   <li>构建字段（服务层依赖注入）</li>
 *   <li>构建方法（Controller 接口实现）</li>
 *   <li>输出 Java 源文件</li>
 * </ul>
 */
public final class ClassCreator {

  private static final String PROCESSOR_NAME = LabzenWebProcessor.class.getName();
  private static final String PROCESSOR_COMMENTS = "labzen web version: " + readVersionFromPom() + ", generating: com.squareup:javapoet, based Java 21";
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final String METHOD_BODY_TEMPLATE_ERROR_INVALID_METHOD = "error-invalid-method";
  private static final String METHOD_BODY_TEMPLATE_GENERAL = "general";
  private static final Pattern METHOD_PARAMETER_PATTERN = Pattern.compile("\\{#parameter[0-9]+}");
  private static final Pattern IMPORT_DIRECTIVE_PATTERN = Pattern.compile("^#import:\\s*(\\S+).*$");

  private final ElementClass root;
  private final Filer filer;
  /** 生成的实现类所实现的原始接口元素，作为 createSourceFile 的 originating element */
  private final TypeElement originatingElement;
  /** 模板 key → 方法体内容（不含 #import 指令行） */
  private final Map<String, String> methodBodyTemplates = Maps.newHashMap();
  /** 模板 key → 该模板声明的 import 全路径类名列表 */
  private final Map<String, List<String>> templateImports = Maps.newHashMap();
  /** 本次生成过程中实际消费的模板 key 集合 */
  private final Set<String> consumedTemplates = new LinkedHashSet<>();

  /**
   * 从 Maven 属性文件中读取版本号
   *
   * @return 版本号，读取失败时返回 "unknown"
   */
  private static String readVersionFromPom() {
    try (var is = ClassCreator.class.getResourceAsStream("/META-INF/maven/cn.labzen/web-processor/pom.properties")) {
      if (is != null) {
        var props = new java.util.Properties();
        props.load(is);
        return props.getProperty("version", "unknown");
      }
    } catch (Exception e) {
      // ignore
    }
    return "unknown";
  }

  public ClassCreator(ElementClass root, Filer filer, TypeElement originatingElement) {
    this.root = root;
    this.filer = filer;
    this.originatingElement = originatingElement;

    loadMethodBodyTemplates();
  }

  /**
   * 加载方法体模板文件
   * <p>
   * 从 classpath 的 templates 目录读取 index.txt 获取模板文件列表，
   * 然后逐个读取模板内容存入缓存。
   */
  private void loadMethodBodyTemplates() {
    if (!methodBodyTemplates.isEmpty()) {
      return;
    }

    try {
      FileObject indexFile = filer.getResource(StandardLocation.CLASS_PATH, "templates", "index.txt");

      try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(indexFile.openInputStream(), StandardCharsets.UTF_8))) {
        String fileName;
        while ((fileName = reader.readLine()) != null) {
          fileName = fileName.trim();
          if (fileName.isEmpty()) {
            continue;
          }

          // 逐个读取 txt 文件
          FileObject txtFile = filer.getResource(StandardLocation.CLASS_PATH, "templates", fileName);

          String rawContent = readContent(txtFile);
          String key = Strings.frontUntil(fileName, ".", false);
          var parsed = parseTemplateImports(rawContent);
          methodBodyTemplates.put(key, parsed.body());
          if (!parsed.imports().isEmpty()) {
            templateImports.put(key, parsed.imports());
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to load the method template", e);
    }
  }

  private String readContent(FileObject fileObject) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(fileObject.openInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * 解析模板中的 {@code #import:} 指令。
   * <p>
   * 模板头部可以声明 import，格式为：
   * <pre>{@code
   *   #import: com.example.MyClass
   *   #import: com.example.AnotherClass
   *   ---
   *   方法体内容...
   * }</pre>
   * {@code ---} 分隔符可选，如果方法体第一行不是 {@code #import:} 则整个内容均为方法体。
   *
   * @param raw 原始模板内容（整段）
   * @return 解析结果：已声明的 imports 列表 + 纯净方法体
   */
  private static TemplateParseResult parseTemplateImports(String raw) {
    List<String> imports = new ArrayList<>();
    String[] lines = raw.split("\n", -1);

    int bodyStart = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      Matcher matcher = IMPORT_DIRECTIVE_PATTERN.matcher(line);
      if (matcher.matches()) {
        imports.add(matcher.group(1));
        bodyStart = i + 1;
      } else if (line.equals("---") && i > 0) {
        // --- 分隔符：如果前面有 import 指令，跳过这行；如果没有 import，视作方法体起始
        bodyStart = imports.isEmpty() ? 0 : i + 1;
        break;
      } else if (!imports.isEmpty()) {
        // 已有 import 指令的前提下，遇到非 import 非 --- 的行 → 方法体开始
        bodyStart = i;
        break;
      } else {
        // 第一行就不是 import → 整个是方法体
        break;
      }
    }

    String body = String.join("\n", Arrays.copyOfRange(lines, bodyStart, lines.length));
    return new TemplateParseResult(imports, body);
  }

  private record TemplateParseResult(List<String> imports, String body) {
  }

  /**
   * 创建 Java 源文件
   * <p>
   * 核心构建流程：
   * <ul>
   *   <li>1. 构建类规范（名称、父接口、修饰符）</li>
   *   <li>2. 添加类注解（包括 @Generated）</li>
   *   <li>3. 构建并添加字段</li>
   *   <li>4. 构建并添加方法</li>
   *   <li>5. 输出到文件</li>
   * </ul>
   */
  public void create() {
    var typeSpecBuilder = TypeSpec.classBuilder(root.getName())
      .addSuperinterface(root.getImplementTypes())
      .addModifiers(Modifier.PUBLIC);

    root.getAnnotations().stream()
      .sorted(Comparator.comparing(ann -> Utils.getSimpleName(ann.getType())))
      .forEach(ann -> typeSpecBuilder.addAnnotation(buildAnnotationSpec(ann)));

    typeSpecBuilder.addAnnotation(
      AnnotationSpec.builder(Generated.class)
        .addMember("value", "$S", PROCESSOR_NAME)
        .addMember("date", "$S", OffsetDateTime.now().format(DATETIME_FORMATTER))
        .addMember("comments", "$S", PROCESSOR_COMMENTS)
        .build());

    var defaultFieldElement = root.getFields().getFirst();

    root.getFields().forEach(field -> {
      var fieldSpecBuilder = FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE);
      field.getAnnotations().forEach(ann -> fieldSpecBuilder.addAnnotation(buildAnnotationSpec(ann)));

      typeSpecBuilder.addField(fieldSpecBuilder.build());
    });

    root.getMethods().forEach(method -> {
      var methodSpecBuilder = MethodSpec.methodBuilder(method.getName())
        .addModifiers(Modifier.PUBLIC)
        .returns(method.getReturnType());

      method.getAnnotations().forEach(annotation -> methodSpecBuilder.addAnnotation(buildAnnotationSpec(annotation)));

      method.getParameters().forEach(parameter -> {
        var parameterSpecBuilder = ParameterSpec.builder(parameter.getType(), parameter.getName());
        parameter.getAnnotations()
          .forEach(annotation -> parameterSpecBuilder.addAnnotation(buildAnnotationSpec(annotation)));

        methodSpecBuilder.addParameter(parameterSpecBuilder.build());
      });

      var body = buildMethodBody(method, defaultFieldElement);
      methodSpecBuilder.addCode(body + "\n");
      typeSpecBuilder.addMethod(methodSpecBuilder.build());
    });

    var typeSpec = typeSpecBuilder.build();
    var javaFile = JavaFile.builder(root.getPkg(), typeSpec).build();
    String source = injectTemplateImports(javaFile.toString());
    output(source);
  }

  /**
   * 将本次生成过程中实际消费的模板所声明的 import 注入到源码中，
   * 插入在 package 声明行之后、类定义之前。
   */
  private String injectTemplateImports(String source) {
    Set<String> all = new LinkedHashSet<>();
    for (String key : consumedTemplates) {
      List<String> imports = templateImports.get(key);
      if (imports != null) {
        all.addAll(imports);
      }
    }
    if (all.isEmpty()) {
      return source;
    }

    int firstNewline = source.indexOf('\n');
    String packageLine = source.substring(0, firstNewline + 1);
    String rest = source.substring(firstNewline + 1);

    StringBuilder block = new StringBuilder();
    for (String imp : all) {
      block.append("import ").append(imp).append(";\n");
    }
    return packageLine + "\n" + block + "\n" + rest;
  }

  /**
   * 输出 Java 源文件
   * <p>
   * 根据系统属性 JUNIT_OUTPUT_DIR 判断运行环境：
   * <ul>
   *   <li>若设置了该属性，则输出到指定目录（用于测试）</li>
   *   <li>否则使用 Filer 输出到编译目标目录</li>
   * </ul>
   *
   * @param source 完整的 Java 源代码字符串
   */
  private void output(String source) {
    var outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "");
    if (!outputTarget.isBlank()) {
      // 构造完整路径：target/{outputTarget}/{package/ClassName}.java
      String relativePath = root.getPkg().replace('.', '/') + "/" + root.getName() + ".java";
      var outputPath = Paths.get("target", outputTarget, relativePath);
      try {
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, source);
      } catch (Exception e) {
        throw new RuntimeException("Failed to write Java file to: " + outputPath, e);
      }
    } else {
      try {
        String qualifiedName = root.getPkg() + "." + root.getName();
        JavaFileObject filerSourceFile = filer.createSourceFile(qualifiedName, originatingElement);
        try (Writer writer = filerSourceFile.openWriter()) {
          writer.write(source);
        }
        // 诊断模式：通过 -Alabzen.web.processor.dump=true 将生成的源码输出到 target/labzen-dump/
        dumpSourceIfEnabled(source);
      } catch (Exception e) {
        throw new RuntimeException("Failed to write Java file using Filer", e);
      }
    }
  }

  /**
   * 诊断模式：将生成的源码副本写入磁盘，方便排查编译问题。
   * 通过编译参数 {@code -Alabzen.web.processor.dump=true} 启用。
   */
  private void dumpSourceIfEnabled(String source) {
    if (!"true".equals(System.getProperty("labzen.web.processor.dump", ""))) {
      return;
    }
    try {
      String relativePath = root.getPkg().replace('.', '/') + "/" + root.getName() + ".java";
      var dumpPath = Paths.get("target", "labzen-dump", relativePath);
      Files.createDirectories(dumpPath.getParent());
      Files.writeString(dumpPath, source);
    } catch (Exception e) {
      // 诊断写入失败不应影响编译流程
    }
  }

  /**
   * 构建方法体内容
   * <p>
   * 核心逻辑：
   * <ul>
   *   <li>1. 检查方法是否有调用目标，无则返回错误模板</li>
   *   <li>2. 确定字段名和方法名</li>
   *   <li>3. 从模板中获取方法体内容</li>
   *   <li>4. 替换模板中的占位符（{#field}、{#method}、{#parameters}）</li>
   *   <li>5. 处理参数索引占位符（{#parameter0}、{#parameter1} 等）</li>
   * </ul>
   *
   * @param method 方法元素
   * @param defaultFieldElement 默认字段元素
   * @return 方法体代码字符串
   */
  private String buildMethodBody(ElementMethod method, ElementField defaultFieldElement) {
    boolean healthyBody = true;
    if (method.getBody().getInvokeMethodName().isEmpty()) {
      // 约定如果没有调用方法，则废弃该方法
      return methodBodyTemplates.get(METHOD_BODY_TEMPLATE_ERROR_INVALID_METHOD);
    }

    var fieldName = method.getBody().getFieldName().isBlank()
      ? defaultFieldElement.getName()
      : method.getBody().getFieldName();
    var methodName = method.getBody().getInvokeMethodName();
    List<String> parameters = method.getParameters().stream().map(ElementParameter::getName).toList();
    var parameterNames = String.join(", ", parameters);

    String body = methodBodyTemplates.get(method.getName());
    if (!Strings.isBlank(body)) {
      consumedTemplates.add(method.getName());
    } else {
      body = methodBodyTemplates.get(METHOD_BODY_TEMPLATE_GENERAL);
      if (!Strings.isBlank(body)) {
        consumedTemplates.add(METHOD_BODY_TEMPLATE_GENERAL);
      }
    }
    if (Strings.isBlank(body)) {
      healthyBody = false;
      body = "throw new IllegalStateException(\"无法确定方法体内容\");";
    }

    if (healthyBody) {
      body = body.replace("{#field}", fieldName);
      body = body.replace("{#method}", methodName);
      body = body.replace("{#parameters}", parameterNames);

      Matcher matcher = METHOD_PARAMETER_PATTERN.matcher(body);
      StringBuilder bodySB = new StringBuilder();
      while (matcher.find()) {
        String group = matcher.group(0);
        String substring = group.substring(11, group.length() - 1);
        int index = Integer.parseInt(substring);
        String value = parameters.get(index);
        matcher.appendReplacement(bodySB, value);
      }
      matcher.appendTail(bodySB);

      body = bodySB.toString();
    }

    return body;
  }

  /**
   * 构建注解规范
   *
   * @param annotation 注解元素
   * @return 注解规范对象
   */
  private AnnotationSpec buildAnnotationSpec(ElementAnnotation annotation) {
    var annotationSpecBuilder = AnnotationSpec.builder((ClassName) annotation.getType());
    annotation.getMembers().forEach((key, value) -> {
      switch (value) {
        case List<?> list -> {
          var block = buildAnnotationArrayMemberValue(list);
          annotationSpecBuilder.addMember(key, "$L", block);
        }
        case Object[] array -> {
          var block = buildAnnotationArrayMemberValue(List.of(array));
          annotationSpecBuilder.addMember(key, "$L", block);
        }
        case null -> {
          /* ignore null values */
        }
        /* 字面量——不带引号直接输出 */
        case Boolean bool -> annotationSpecBuilder.addMember(key, "$L", bool);
        case Number number -> annotationSpecBuilder.addMember(key, "$L", number);
        case Character ch -> annotationSpecBuilder.addMember(key, "'$L'", ch);
        /* 字符串 */
        case String s -> annotationSpecBuilder.addMember(key, "$S", s);
        /* 枚举常量 → e.g. RequestMethod.GET */
        case VariableElement ve -> annotationSpecBuilder.addMember(key, "$T.$L",
            ClassName.get((TypeElement) ve.getEnclosingElement()), ve.getSimpleName());
        /* Class 字面量 → e.g. IOException.class */
        case TypeMirror tm -> annotationSpecBuilder.addMember(key, "$T.class", tm);
        /* 嵌套注解 */
        case AnnotationMirror nested -> {
          var nestedSpec = buildAnnotationSpec(new ElementAnnotation(
              ClassName.get((TypeElement) nested.getAnnotationType().asElement()),
              Utils.readAnnotationMembers(nested)));
          annotationSpecBuilder.addMember(key, "$L", nestedSpec);
        }
        default -> throw new IllegalArgumentException(
            "unsupported annotation member type: " + value.getClass().getName() + " for key '" + key + "'");
      }
    });
    return annotationSpecBuilder.build();
  }

  /**
   * 构建注解的数组类型成员值
   * <p>
   * 将 List 或数组转换为注解的数组语法，如 { "value1", "value2" }
   *
   * @param values 值列表
   * @return 代码块对象
   */
  private CodeBlock buildAnnotationArrayMemberValue(List<?> values) {
    var block = CodeBlock.builder().add("{");
    for (int i = 0; i < values.size(); i++) {
      var ele = values.get(i);
      block.add("$S", ele.toString());
      if (i != values.size() - 1) {
        block.add(",");
      }
    }
    block.add("}");
    return block.build();
  }
}