package cn.labzen.web.ap.internal

import com.squareup.javapoet.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror


class Utils private constructor() {

  companion object {

    private val REQUEST_MAPPING_ANNOTATIONS = setOf(
      "org.springframework.web.bind.annotation.RequestMapping",
      "org.springframework.web.bind.annotation.GetMapping",
      "org.springframework.web.bind.annotation.PostMapping",
      "org.springframework.web.bind.annotation.DeleteMapping",
      "org.springframework.web.bind.annotation.PutMapping",
      "org.springframework.web.bind.annotation.PatchMapping",
    )

    fun isRequestMappingAnnotation(className: ClassName): Boolean =
      REQUEST_MAPPING_ANNOTATIONS.contains(className.canonicalName())

    fun readAnnotationMembers(annotation: AnnotationMirror): Map<String, Any?> =
      annotation.elementValues.entries.associate { (element, value) ->
        val name = element.simpleName.toString()
        val actualValue = value.value
        if (actualValue is List<*>) {
          Pair(name, actualValue.map { (it as AnnotationValue).value })
        } else {
          Pair(name, actualValue)
        }
      }

    fun classOf(type: Element): ClassName =
      ClassName.get(type as TypeElement)

    fun typeOf(type: TypeMirror): TypeName {
      return TypeName.get(type)
    }

    fun getSimpleName(type: TypeName): String {
      return when (type) {
        is ClassName -> type.simpleName()
        is ArrayTypeName -> getSimpleName(type.componentType) + "[]"
        is ParameterizedTypeName -> {
          val raw = getSimpleName(type.rawType)
          val typeArgs = type.typeArguments.joinToString(", ") { getSimpleName(it) }
          "$raw<$typeArgs>"
        }

        is TypeVariableName -> type.name
        is WildcardTypeName -> {
          val out = type.upperBounds
          val `in` = type.lowerBounds
          when {
            `in`.isNotEmpty() -> "? super ${getSimpleName(`in`[0])}"
            out.isNotEmpty() && out[0] != ClassName.OBJECT -> "? extends ${getSimpleName(out[0])}"
            else -> "?"
          }
        }

        else -> type.toString()
      }
    }
  }
}