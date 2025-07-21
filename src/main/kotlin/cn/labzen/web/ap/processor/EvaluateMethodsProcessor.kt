package cn.labzen.web.ap.processor

import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator
import cn.labzen.web.ap.internal.Utils
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.internal.element.*
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_EVALUATE_METHODS
import cn.labzen.web.ap.suggestion.impl.AppendSuggestion
import cn.labzen.web.ap.suggestion.impl.DiscardSuggestion
import cn.labzen.web.ap.suggestion.impl.RemoveSuggestion
import cn.labzen.web.ap.suggestion.impl.ReplaceSuggestion
import com.squareup.javapoet.TypeName
import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Types

class EvaluateMethodsProcessor : InternalProcessor {

  private val parsedMethods = mutableMapOf<String, ElementMethod>()
  private lateinit var types: Types
  private lateinit var rootElement: ElementClass
  private lateinit var config: Config
  private lateinit var evaluates: List<MethodErasableAnnotationEvaluator>
  private lateinit var sourceType: DeclaredType

  override fun process(context: ControllerContext) {
    with(context) {
      types = apc.typeUtils
      rootElement = root
      config = apc.config
      evaluates = annotationEvaluators
      sourceType = source.asType() as DeclaredType
    }

    collectMethods(context, context.source)

    parsedMethods.forEach { (_, value) ->
      rootElement.methods.add(value)
    }
  }

  private fun collectMethods(context: ControllerContext, source: TypeElement) {
    val directSupertypes = types.directSupertypes(source.asType())
    directSupertypes.forEach { superType ->
      // 如果controller的父类型不是LabzenController及其子接口，则不处理
      val supportInterface = types.isAssignable(superType, context.ancestorControllerType)
      if (!supportInterface) {
        return@forEach
      }

      val superElement = (superType as DeclaredType).asElement() as TypeElement
      collectMethods(context, superElement)
    }

    ElementFilter.methodsIn(source.enclosedElements).forEach { method ->
      parseMethod(method)
    }
  }

  private fun parseMethod(method: ExecutableElement) {
    val methodName = method.simpleName.toString()
    val returnType = Utils.typeOf(method.returnType)

    val resolvedMethodType = types.asMemberOf(sourceType, method) as ExecutableType
    // 真实的方法参数类型
    val actualParameterTypes = resolvedMethodType.parameterTypes
    val parameterTypeNames = actualParameterTypes.map {
      val type = Utils.typeOf(it)
      Utils.getSimpleName(type)
    }
    val parameterNames = method.parameters.map { it.simpleName.toString() }
    val parametersSignature = parameterTypeNames.joinToString(", ")
    val methodSignature = "${Utils.getSimpleName(returnType)} $methodName($parametersSignature)"

    val methodElement = parsedMethods.computeIfAbsent(methodSignature) {
      ElementMethod(methodName, returnType).apply {
        body = ElementMethodBody("", methodName, parameterNames)
      }
    }

    // 读取所有参数（及注解）
    val methodParameters = readParameters(method.parameters, actualParameterTypes)
    methodParameters.forEach { read ->
      val superParameterExists = methodElement.parameters.contains(read)
      if (!superParameterExists) {
        methodElement.parameters.add(read)
        return@forEach
      }

      // 重写的方法，参数列表需要覆盖掉父接口中定义的注解集合
      val found = methodElement.parameters.find { it == read }!!
      found.annotations.addAll(read.annotations)
    }
    methodElement.parameters.addAll(methodParameters)

    // 读取所有的方法注解
    val methodAnnotations = method.annotationMirrors.filterNot { mirror ->
      // 过滤掉 jetbrains 的注解，主要是 NotNull 和 Nullable，强制使用 JSR305 的 javax.annotation 下的注解
      val fqcn = mirror.annotationType.asElement().toString()
      fqcn.startsWith("org.jetbrains.annotations.")
    }.map { mirror ->
      val annotationClass = Utils.typeOf(mirror.annotationType.asElement().asType())
      val annotationMembers = Utils.readAnnotationMembers(mirror)

      ElementAnnotation(annotationClass, annotationMembers.toMutableMap())
    }
    methodElement.annotations.addAll(methodAnnotations)

    parseMethodAnnotations(methodElement)
  }

  private fun readParameters(parameters: List<VariableElement>, actualParameterTypes: List<TypeMirror>) =
    parameters.mapIndexed { i, param ->
      val parameterName = param.simpleName.toString()
      val parameterType = Utils.typeOf(actualParameterTypes[i])

      val annotations = readAnnotations(param.annotationMirrors)
      ElementParameter(i, parameterName, parameterType, LinkedHashSet(annotations))
    }

  private fun readAnnotations(annotationMirrors: List<AnnotationMirror>) =
    annotationMirrors.map { mirror ->
      val annotationClass = Utils.classOf(mirror.annotationType.asElement())
      val annotationMembers = Utils.readAnnotationMembers(mirror)

      ElementAnnotation(annotationClass, annotationMembers.toMutableMap())
    }

  private fun parseMethodAnnotations(method: ElementMethod) {
    val suggestions = method.annotations.flatMap { annotation ->
      evaluates.flatMap { evaluator ->
        if (evaluator.support(annotation.type)) {
          evaluator.evaluate(config, annotation.type, annotation.members)
        } else listOf()
      }
    }

    suggestions.forEach { suggestion ->
      when (suggestion) {
        is AppendSuggestion -> parseAppendSuggestion(method, suggestion)
        is RemoveSuggestion -> parseRemoveSuggestion(method, suggestion)
        is ReplaceSuggestion -> parseReplaceSuggestion(method, suggestion)
        is DiscardSuggestion -> parseDiscardSuggestion(method)
      }
    }
  }

  private fun parseAppendSuggestion(method: ElementMethod, suggestion: AppendSuggestion) {
    when (suggestion.element) {
      is ElementField -> {
        rootElement.fields.add(suggestion.element)
      }

      is ElementAnnotation -> {
        if (suggestion.kind == ElementClass::class.java) {
          rootElement.annotations.add(suggestion.element)
        } else if (suggestion.kind == ElementMethod::class.java) {
          method.annotations.add(suggestion.element)
        }
      }
    }
  }

  private fun parseRemoveSuggestion(method: ElementMethod, suggestion: RemoveSuggestion) {
    when (suggestion.kind) {
      ElementClass::class.java -> {
        rootElement.fields.removeIf {
          it.keyword() == suggestion.keyword
        }
        rootElement.annotations.removeIf {
          it.keyword() == suggestion.keyword
        }
      }

      ElementMethod::class.java -> {
        method.annotations.removeIf {
          it.keyword() == suggestion.keyword
        }
      }
    }
  }

  private fun parseReplaceSuggestion(method: ElementMethod, suggestion: ReplaceSuggestion) {
    val element = suggestion.element
    when (element) {
      is ElementMethodBody -> {
        val fieldName = element.fieldName.ifBlank { method.body.fieldName }
        val invokeMethodName = element.invokeMethodName.ifBlank { method.body.invokeMethodName }
        val parameterNames = element.parameterNames.ifEmpty { method.body.parameterNames }

        method.body = ElementMethodBody(fieldName, invokeMethodName, parameterNames)
      }

      is ElementAnnotation -> {
        method.annotations.find {
          it.keyword() == suggestion.keyword
        }?.apply {
          members.putAll(element.members)
        }
      }
    }
  }

  private fun parseDiscardSuggestion(method: ElementMethod) {
    // 移除方法上的注解，忽略Override, Nonnull等
    method.annotations.removeIf {
      !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(it.type)
    }
    method.parameters.forEach { parameter ->
      // 移除方法参数的注解，忽略Override, Nonnull等
      parameter.annotations.removeIf {
        !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(it.type)
      }
    }
    method.body = ElementMethodBody("", "", emptyList())
  }

  override fun priority(): Int = PRIORITY_EVALUATE_METHODS

  companion object {
    private val RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD = setOf(
      TypeName.get(Override::class.java),
      TypeName.get(Nonnull::class.java),
      TypeName.get(Nullable::class.java),
    )
  }
}