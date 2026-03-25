package cn.labzen.web.apt.internal;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.LabzenWebProcessor;
import cn.labzen.web.apt.internal.element.*;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.labzen.web.apt.definition.UnitConstants.JUNIT_OUTPUT_DIR;

public final class ClassCreator {

  private static final String PROCESSOR_NAME = LabzenWebProcessor.class.getName();
  private static final String PROCESSOR_COMMENTS = "labzen web version: 1.2.0, generating: com.squareup:javapoet, based Java 21";
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final String METHOD_BODY_TEMPLATE_ERROR = "error";
  private static final String METHOD_BODY_TEMPLATE_GENERAL = "general";
  private static final Pattern METHOD_PARAMETER_PATTERN = Pattern.compile("\\{#parameter[0-9]+}");

  private final ElementClass root;
  private final Filer filer;
  private final Map<String, String> methodBodyTemplates = Maps.newHashMap();

  public ClassCreator(ElementClass root, Filer filer) {
    this.root = root;
    this.filer = filer;

    loadMethodBodyTemplates();
  }

  private void loadMethodBodyTemplates() {
    try {
      Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("templates");
      if (!resources.hasMoreElements()) {
        return;
      }

      URL resource = resources.nextElement();
      String protocol = resource.getProtocol();

      // 处理 jar 包和文件系统的不同情况
      if ("jar".equalsIgnoreCase(protocol)) {
        String path = resource.getPath();
        // jar 包内的资源，使用 JarFile 读取
        String jarPath = path.substring(0, path.indexOf("!"));
        URI jarURI = new URI(jarPath);
        File file = new File(jarURI);

        try (JarFile jarFile = new JarFile(file)) {
          var entries = jarFile.entries();
          while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.getName().startsWith("templates/") && entry.getName().endsWith(".txt")) {
              String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
              String key = Strings.frontUntil(fileName, ".", false);
              try (InputStream is = jarFile.getInputStream(entry);
                   InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                   BufferedReader reader = new BufferedReader(isr)) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                methodBodyTemplates.put(key, content);
              }
            }
          }
        }
      } else if ("file".equalsIgnoreCase(protocol)) {
        // 文件系统资源，使用 Files.list 读取
        Path controllerDir = Paths.get(resource.toURI());
        try (var stream = Files.list(controllerDir)) {
          stream.filter(p -> p.toString().endsWith(".txt"))
            .forEach(p -> {
              try {
                String fileName = p.getFileName().toString();
                String key = Strings.frontUntil(fileName, ".", false);
                String content = Files.readString(p, StandardCharsets.UTF_8);
                methodBodyTemplates.put(key, content);
              } catch (IOException e) {
                throw new RuntimeException("读取文件失败：" + p, e);
              }
            });
        }
      } else {
        throw new RuntimeException("未知的资源协议：" + protocol);
      }
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("加载方法体模板失败", e);
    }
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
    if (method.getBody().getInvokeMethodName().isEmpty()) {
      // 约定如果没有调用方法，则废弃该方法
      return methodBodyTemplates.get(METHOD_BODY_TEMPLATE_ERROR);
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
      body = "";
    }

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

    return bodySB.toString();
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