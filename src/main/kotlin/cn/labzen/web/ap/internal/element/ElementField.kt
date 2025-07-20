package cn.labzen.web.ap.internal.element

import cn.labzen.web.ap.internal.Utils
import com.squareup.javapoet.TypeName

data class ElementField(
  val name: String,
  val type: TypeName,
  val annotations: List<ElementAnnotation>,
) : Element {

  override fun keyword(): String =
    "${Utils.getSimpleName(type)} $name"

  override fun toString(): String =
    keyword()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ElementField

    if (name != other.name) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + type.hashCode()
    return result
  }
}