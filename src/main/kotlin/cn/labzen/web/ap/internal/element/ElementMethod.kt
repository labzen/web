package cn.labzen.web.ap.internal.element

import com.squareup.javapoet.TypeName

data class ElementMethod(
  val name: String,
  val returnType: TypeName,
) : Element {

  val parameters = LinkedHashSet<ElementParameter>()
  val annotations = LinkedHashSet<ElementAnnotation>()
  lateinit var body: ElementMethodBody

  override fun keyword(): String =
    parameters.sortedBy { it.index }
      .joinToString(", ") { it.keyword() }
      .let { "$returnType $name($it)" }

  override fun toString(): String =
    keyword()
}