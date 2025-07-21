package cn.labzen.web.ap.internal.element

import cn.labzen.web.ap.internal.Utils
import com.squareup.javapoet.TypeName

data class ElementParameter(
  val index: Int,
  val name: String,
  val type: TypeName,
  val annotations: LinkedHashSet<ElementAnnotation>,
) : Element {

  override fun keyword(): String =
    Utils.getSimpleName(type)

  override fun toString(): String =
    keyword()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ElementParameter

    if (index != other.index) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = index
    result = 31 * result + type.hashCode()
    return result
  }
}