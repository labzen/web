package cn.labzen.web.ap.processor

import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.internal.element.ElementField
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_EVALUATE_FIELDS
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion
import com.squareup.javapoet.TypeName
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * 解析Controller父接口声明的泛型，转换为Controller实现类中需要添加的Field
 */
class EvaluateFieldsProcessor : InternalProcessor {

  override fun process(context: ControllerContext) {
    // 读取所有继承的父接口定义的泛型参数类型
    val directSupertypes = context.apc.typeUtils.directSupertypes(context.source.asType())

    directSupertypes.forEach { inter ->
      val typeArguments = detectTypeArguments(inter) ?: return@forEach

      val interfaceClassName: TypeName = Utils.typeOf(inter)

      // 遍历每一个评价器
      val suggestions = context.genericsEvaluators.flatMap { evaluator ->
        if (evaluator.support(interfaceClassName)) {
          evaluator.evaluate(typeArguments)
        } else listOf()
      }

      suggestions.forEach { suggestion ->
        when (suggestion) {
          is AppendSuggestion -> parseAppendSuggestion(context.root, suggestion)
          is RemoveSuggestion -> parseRemoveSuggestion(context.root, suggestion)
          is ReplaceSuggestion -> parseReplaceSuggestion(context.root, suggestion)
        }
      }
    }
  }

  private fun parseAppendSuggestion(root: ElementClass, suggestion: AppendSuggestion) {
    when (suggestion.element) {
      is ElementField -> {
        root.fields.add(suggestion.element)
      }

      is ElementAnnotation -> {
        if (suggestion.kind == ElementClass::class.java) {
          root.annotations.add(suggestion.element)
        }
      }
    }
  }

  private fun parseRemoveSuggestion(root: ElementClass, suggestion: RemoveSuggestion) {
    if (suggestion.kind != ElementClass::class.java) {
      return
    }
    root.fields.removeIf {
      it.keyword() == suggestion.keyword
    }
    root.annotations.removeIf {
      it.keyword() == suggestion.keyword
    }
  }

  private fun parseReplaceSuggestion(root: ElementClass, suggestion: ReplaceSuggestion) {
    val element = suggestion.element
    when (element) {
      is ElementAnnotation -> {
        root.annotations.find {
          it.keyword() == suggestion.keyword
        }?.apply {
          members.putAll(element.members)
        }
      }
    }
  }

  private fun detectTypeArguments(typeMirror: TypeMirror): List<TypeName>? =
    if (typeMirror is DeclaredType) {
      val typeArguments = typeMirror.typeArguments
      typeArguments.map { Utils.typeOf(it) }
    } else null

  override fun priority(): Int = PRIORITY_EVALUATE_FIELDS

}