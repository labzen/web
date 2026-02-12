package cn.labzen.web.apt.internal;

import cn.labzen.web.apt.LabzenWebProcessor;
import cn.labzen.web.apt.internal.element.*;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static cn.labzen.web.apt.definition.UnitConstants.JUNIT_OUTPUT_DIR;

public final class ClassCreator {

  private static final String PROCESSOR_NAME = LabzenWebProcessor.class.getName();
  private static final String PROCESSOR_COMMENTS = "labzen web version: 1.2.0, generating: com.squareup:javapoet, based Java 21";
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  private final ElementClass root;
  private final Filer filer;

  public ClassCreator(ElementClass root, Filer filer) {
    this.root = root;
    this.filer = filer;
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
      methodSpecBuilder.addCode(body);
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
      return "throw new UnsupportedOperationException();\n";
    }

    var parameterNames = method.getParameters().stream().map(ElementParameter::getName)
      .collect(Collectors.joining(", "));

    var fieldName = method.getBody().getFieldName().isBlank()
      ? defaultFieldElement.getName()
      : method.getBody().getFieldName();

    var methodName = method.getBody().getInvokeMethodName();
    return "return " + fieldName + "." + methodName + "(" + parameterNames + ");\n";
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