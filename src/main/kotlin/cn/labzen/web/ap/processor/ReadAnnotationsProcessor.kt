package cn.labzen.web.ap.processor

import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.internal.element.ElementAnnotation
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.internal.element.ElementField
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_READ_ANNOTATION
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion

/**
 * 读取Controller接口的所有注解
 */
class ReadAnnotationsProcessor : InternalProcessor {

  override fun process(context: ControllerContext) {
    // 获取接口声明的所有注解，将直接复制给生成的Controller实现类
    context.source.annotationMirrors.forEach {
      val annotationClass = Utils.elementToClass(it.annotationType.asElement())
      val annotationMembers = Utils.readAnnotationMembers(it)

      val annotation = ElementAnnotation(annotationClass, annotationMembers.toMutableMap())
      context.root.annotations.add(annotation)

      val suggestions = context.annotationEvaluators.flatMap { evaluator ->
        if (evaluator.support(annotation.type)) {
          evaluator.evaluate(context.apc.config, annotation.type, annotation.members)
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


  override fun priority(): Int = PRIORITY_READ_ANNOTATION
}