package cn.labzen.web.ap.internal.context

import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator
import cn.labzen.web.ap.evaluate.generics.InterfaceGenericsEvaluator
import cn.labzen.web.ap.internal.element.ElementClass
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

data class ControllerContext(
  val source: TypeElement,
  val apc: AnnotationProcessorContext
) {

  val ancestorControllerType: TypeMirror = apc.elementUtils.getTypeElement(ANCESTOR_NAME).asType()

  lateinit var genericsEvaluators: List<InterfaceGenericsEvaluator>
  lateinit var annotationEvaluators: List<MethodErasableAnnotationEvaluator>

  lateinit var root: ElementClass

  companion object {
    private const val ANCESTOR_NAME = "cn.labzen.web.controller.LabzenController"
  }
}