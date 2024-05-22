package cn.labzen.web.source

import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.*
import kotlin.Annotation
import kotlin.reflect.full.declaredMembers
import javassist.bytecode.annotation.Annotation as CTAnnotation

/**
 * Controller接口实现类代码生成器助手，基于 javassist 对类生成的操作
 */
internal object ControllerSourceGeneratorHelper {

  /**
   * 将注解集合，转换为可附加到类上的运行时可见注解属性
   */
  fun duplicateAnnotations(interfaceAnnotations: Array<Annotation>, constPool: ConstPool): AnnotationsAttribute {
    val ctAnnotations = interfaceAnnotations.map { ia -> duplicateAnnotation(ia, constPool) }

    // 运行时可见的注解属性
    val annotationsAttribute =
      AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    ctAnnotations.forEach { annotationsAttribute.addAnnotation(it) }
    return annotationsAttribute
  }

  /**
   * 将原生代码中的注解信息，转换为 javassist 的注解
   */
  fun duplicateAnnotation(interfaceAnnotation: Annotation, constPool: ConstPool): CTAnnotation {
    val iaClass = interfaceAnnotation.annotationClass
    val ctAnnotation = CTAnnotation(iaClass.qualifiedName, constPool)

    val iaMembers = iaClass.declaredMembers
    iaMembers.forEach { iam ->
      val value = iam.call(interfaceAnnotation)
      val ctAnnotationMemberValue: MemberValue? = value?.let { parseAnnotationMemberValue(it, constPool) }
      ctAnnotationMemberValue?.apply {
        ctAnnotation.addMemberValue(iam.name, ctAnnotationMemberValue)
      }
    }
    return ctAnnotation
  }

  /**
   * 将原生代码的注解属性值，转换为 javassist 的注解值
   */
  private fun parseAnnotationMemberValue(value: Any, constPool: ConstPool): MemberValue {
    return when (value) {
      is String -> StringMemberValue(value, constPool)
      is Int -> IntegerMemberValue(constPool, value)
      is Short -> ShortMemberValue(value, constPool)
      is Long -> LongMemberValue(value, constPool)
      is Boolean -> BooleanMemberValue(value, constPool)
      is Byte -> ByteMemberValue(value, constPool)
      is Double -> DoubleMemberValue(value, constPool)
      is Float -> FloatMemberValue(value, constPool)
      is Char -> CharMemberValue(value, constPool)

      is Enum<*> -> EnumMemberValue(constPool).apply {
        this.type = value.javaClass.name
        this.value = value.name
      }

      is Class<*> -> ClassMemberValue(value.name, constPool)

      is Annotation -> AnnotationMemberValue(duplicateAnnotation(value, constPool), constPool)

      is Array<*> -> {
        val arrayMemberValue = ArrayMemberValue(constPool)
        arrayMemberValue.value = value.mapNotNull {
          if (it != null) {
            parseAnnotationMemberValue(it, constPool)
          } else null
        }.toTypedArray()
        return arrayMemberValue
      }

      else -> throw IllegalArgumentException("不支持的注解参数 - ${value.javaClass}")
    }
  }
}