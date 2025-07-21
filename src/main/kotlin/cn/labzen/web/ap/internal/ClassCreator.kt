package cn.labzen.web.ap.internal

import cn.labzen.web.JUNIT_OUTPUT_DIR
import cn.labzen.web.ap.LabzenWebProcessor
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.internal.element.ElementField
import cn.labzen.web.ap.internal.element.ElementMethod
import com.squareup.javapoet.*
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.annotation.processing.Filer
import javax.annotation.processing.Generated
import javax.lang.model.element.Modifier


class ClassCreator(
  private val root: ElementClass,
  private val filer: Filer
) {

  fun create() {
    val typeSpecBuilder =
      TypeSpec.classBuilder(root.name).addSuperinterface(root.implements).addModifiers(Modifier.PUBLIC)
    root.annotations.sortedBy { Utils.getSimpleName(it.type) }.forEach { annotation ->
      typeSpecBuilder.addAnnotation(buildAnnotationSpec(annotation))
    }
    typeSpecBuilder.addAnnotation(
      AnnotationSpec.builder(Generated::class.java)
        .addMember("value", "\$S", PROCESSOR_NAME)
        .addMember("date", "\$S", OffsetDateTime.now().format(DATETIME_FORMATTER))
        .addMember("comments", "\$S", PROCESSOR_COMMENTS)
        .build()
    )

    val defaultFieldElement = root.fields.first()

    root.fields.forEach { field ->

      val fieldSpecBuilder = FieldSpec.builder(field.type, field.name, Modifier.PRIVATE)
      field.annotations.forEach { annotation ->
        fieldSpecBuilder.addAnnotation(buildAnnotationSpec(annotation))
      }

      typeSpecBuilder.addField(fieldSpecBuilder.build())
    }

    root.methods.forEach { method ->
      val methodSpecBuilder = MethodSpec.methodBuilder(method.name)
        .addModifiers(Modifier.PUBLIC)
        .returns(method.returnType)

      method.annotations.forEach { annotation ->
        methodSpecBuilder.addAnnotation(buildAnnotationSpec(annotation))
      }

      method.parameters.forEach { parameter ->
        val parameterSpecBuilder = ParameterSpec.builder(parameter.type, parameter.name)
        parameter.annotations.forEach { annotation ->
          parameterSpecBuilder.addAnnotation(buildAnnotationSpec(annotation))
        }
        methodSpecBuilder.addParameter(parameterSpecBuilder.build())
      }

      val body = buildMethodBody(method, defaultFieldElement)
      methodSpecBuilder.addCode(body)

      typeSpecBuilder.addMethod(methodSpecBuilder.build())
    }

    val typeSpec = typeSpecBuilder.build()
    val javaFile = JavaFile.builder(root.pkg, typeSpec).build()

    output(javaFile)
  }

  private fun output(javaFile: JavaFile) {
    val outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "")
    if (outputTarget.isNotBlank()) {
      val outputPath = Paths.get("target/$outputTarget")
      javaFile.writeTo(outputPath)
    } else {
      javaFile.writeTo(filer)
    }
  }

  private fun buildMethodBody(method: ElementMethod, defaultFieldElement: ElementField): String {
    if (method.body.invokeMethodName.isEmpty()) {
      // 约定如果没有调用方法，则废弃该方法
      return "throw new UnsupportedOperationException();\n"
    }

    val parameterNames = method.parameters.joinToString(", ") { it.name }
    val fieldName = method.body.fieldName.ifBlank { defaultFieldElement.name }
    val methodName = method.body.invokeMethodName
    return "return ${fieldName}.$methodName($parameterNames);\n"
  }

  private fun buildAnnotationSpec(annotation: ElementAnnotation): AnnotationSpec {
    val annotationSpecBuilder = AnnotationSpec.builder(annotation.type as ClassName)
    annotation.members.forEach { (key, value) ->
      when (value) {
        is List<*> -> {
          val block = buildAnnotationArrayMemberValue(value)
          annotationSpecBuilder.addMember(key, "\$L", block)
        }

        is Array<*> -> {
          val block = buildAnnotationArrayMemberValue(value.toList())
          annotationSpecBuilder.addMember(key, "\$L", block)
        }

        else -> annotationSpecBuilder.addMember(key, "\$S", value)
      }
    }
    return annotationSpecBuilder.build()
  }

  private fun buildAnnotationArrayMemberValue(values: List<*>): CodeBlock {
    val block = CodeBlock.builder().add("{")

    values.forEachIndexed { index, ele ->
      block.add("\$S", ele.toString())
      if (index != values.lastIndex) {
        block.add(",")
      }
    }
    block.add("}")
    return block.build()
  }

  companion object {
    private val PROCESSOR_NAME = LabzenWebProcessor::class.java.name
    private const val PROCESSOR_COMMENTS = "labzen web version: 1.1.0, generating: com.squareup:javapoet, based Java 11"
    private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  }
}