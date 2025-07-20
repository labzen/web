package cn.labzen.web.ap.evaluate.generics

import cn.labzen.web.ap.suggestion.Suggestion
import cn.labzen.web.controller.StandardController
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class StandardControllerInterfaceGenericsEvaluator : PrimaryServiceGenericAssign(), InterfaceGenericsEvaluator {

  override fun support(type: TypeName): Boolean =
    if (type is ParameterizedTypeName) {
      TYPE == type.rawType
    } else {
      TYPE == type
    }

  override fun evaluate(arguments: List<TypeName>): List<Suggestion> {
    return internalEvaluate(arguments.first())
  }

  companion object {
    private val TYPE = TypeName.get(StandardController::class.java)
  }
}