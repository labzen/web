package cn.labzen.web.ap.internal.element

import javax.lang.model.type.TypeMirror

data class ElementClass(
  val name: String,
  val pkg: String,
  val implements: TypeMirror
) : Element {

  val annotations = LinkedHashSet<ElementAnnotation>()
  val fields = LinkedHashSet<ElementField>()
  val methods = LinkedHashSet<ElementMethod>()

  override fun keyword(): String =
    "$pkg.$name"
}
