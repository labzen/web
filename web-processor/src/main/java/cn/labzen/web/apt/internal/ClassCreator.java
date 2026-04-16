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

import static cn.labzen.web.apt.definition.UnitConstants.JUNIT_OUTPUT_DIR;

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