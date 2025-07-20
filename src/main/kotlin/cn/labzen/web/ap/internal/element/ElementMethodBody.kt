package cn.labzen.web.ap.internal.element

data class ElementMethodBody(
  val fieldName: String,
  val invokeMethodName: String,
  val parameterNames: List<String>
) : Element {

  override fun keyword(): String =
    "{}"
}