package cn.labzen.web.ap.internal.element

import cn.labzen.web.ap.internal.Utils
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
      .joinToString(",") { Utils.getSimpleName(it.type) }
      .let { "$returnType $name($it)" }

  override fun toString(): String =
    keyword()
}