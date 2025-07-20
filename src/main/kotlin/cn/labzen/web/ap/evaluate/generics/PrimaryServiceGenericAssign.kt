package cn.labzen.web.ap.evaluate.generics

import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.internal.element.ElementField
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import com.squareup.javapoet.TypeName
import javax.annotation.Resource

/**
 * 核心业务逻辑类泛型指向
 */
abstract class PrimaryServiceGenericAssign {

  protected fun internalEvaluate(primaryComponentClass: TypeName): List<AppendSuggestion> {
    val simpleName = Utils.getSimpleName(primaryComponentClass)
    val fieldName = simpleName[0].lowercaseChar() + simpleName.substring(1)
    val annotations = ElementAnnotation(TypeName.get(Resource::class.java))
    val field = ElementField(fieldName, primaryComponentClass, listOf(annotations))

    return listOf(AppendSuggestion(field, ElementClass::class.java))
  }
}