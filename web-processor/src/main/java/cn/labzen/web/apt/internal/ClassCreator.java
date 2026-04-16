package cn.labzen.web.apt.internal;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.LabzenWebProcessor;
import cn.labzen.web.apt.internal.element.*;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

  private final ElementClass root;
  private final Filer filer;
  private final Map<String, String> methodBodyTemplates = Maps.newHashMap();

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

  public ClassCreator(ElementClass root, Filer filer) {
    this.root = root;
    this.filer = filer;

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

          String content = readContent(txtFile);
          String key = Strings.frontUntil(fileName, ".", false);
          methodBodyTemplates.put(key, content);
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
        sb.append(line);
      }
    }
    return sb.toString();
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
    output(javaFile);
  }

  /**
   * 输出 Java 文件
   * <p>
   * 根据系统属性 JUNIT_OUTPUT_DIR 判断运行环境：
   * <ul>
   *   <li>若设置了该属性，则输出到指定目录（用于测试）</li>
   *   <li>否则使用 Filer 输出到编译目标目录</li>
   * </ul>
   *
   * @param javaFile 要输出的 Java 文件对象
   */
  private void output(JavaFile javaFile) {
    var outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "");
    if (!outputTarget.isBlank()) {
      var outputPath = Paths.get("target/" + outputTarget);
      try {
        javaFile.writeTo(outputPath);
      } catch (Exception e) {
        throw new RuntimeException("Failed to write Java file to: " + outputPath, e);
      }
    } else {
      try {
        javaFile.writeTo(filer);
      } catch (Exception e) {
        throw new RuntimeException("Failed to write Java file using Filer", e);
      }
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
    if (Strings.isBlank(body)) {
      body = methodBodyTemplates.get(METHOD_BODY_TEMPLATE_GENERAL);
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
        default -> annotationSpecBuilder.addMember(key, "$S", value.toString());
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