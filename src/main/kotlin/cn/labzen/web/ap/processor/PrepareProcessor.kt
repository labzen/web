package cn.labzen.web.ap.processor

import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator
import cn.labzen.web.ap.evaluate.generics.InterfaceGenericsEvaluator
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_PREPARE
import java.util.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.NestingKind

/**
 * 检查Controller接口合法性
 */
class PrepareProcessor : InternalProcessor {

  override fun process(context: ControllerContext) {
    val source = context.source
    if (source.kind != ElementKind.INTERFACE) {
      context.apc.messages.warning("注解了 @LabzenController 的源文件必须是接口(interface)类")
    }

    if (source.nestingKind != NestingKind.TOP_LEVEL) {
      context.apc.messages.warning("注解了 @LabzenController 的接口必须是顶级类")
    }

    context.genericsEvaluators = getClassGenerators()
    context.annotationEvaluators = getAnnotationEvaluators()
  }

  private fun getClassGenerators(): List<InterfaceGenericsEvaluator> =
    ServiceLoader.load(InterfaceGenericsEvaluator::class.java, this.javaClass.classLoader)
      .toList()

  private fun getAnnotationEvaluators(): List<MethodErasableAnnotationEvaluator> =
    ServiceLoader.load(MethodErasableAnnotationEvaluator::class.java, this.javaClass.classLoader)
      .toList()

  override fun priority(): Int = PRIORITY_PREPARE
}