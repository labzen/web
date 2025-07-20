package cn.labzen.web.ap.internal.element

import cn.labzen.web.ap.internal.Utils
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.springframework.web.bind.annotation.RequestMapping

data class ElementAnnotation(
  val type: TypeName,
  val members: MutableMap<String, Any?> = mutableMapOf(),
) : Element {

  override fun keyword(): String {
    return if (Utils.isRequestMappingAnnotation(type as ClassName)) {
      mappingTypeName
    } else {
      Utils.getSimpleName(type)
    }
  }

  override fun toString(): String =
    keyword()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ElementAnnotation

    return type == other.type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }

  companion object {
    private val mappingTypeName = Utils.getSimpleName(ClassName.get(RequestMapping::class.java))
  }
}
