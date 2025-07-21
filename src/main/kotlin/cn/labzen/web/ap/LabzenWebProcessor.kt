package cn.labzen.web.ap

import cn.labzen.web.ap.config.ConfigLoader
import cn.labzen.web.ap.exception.TypeNotAvailableException
import cn.labzen.web.ap.internal.context.AnnotationProcessorContext
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.processor.InternalProcessor
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementKindVisitor9
import javax.tools.Diagnostic

@SupportedAnnotationTypes("cn.labzen.web.annotation.LabzenController")
class LabzenWebProcessor : AbstractProcessor() {

  private lateinit var annotationProcessorContext: AnnotationProcessorContext
  private val processedControllers = mutableSetOf<TypeElement>()
  private val deferredControllers = mutableSetOf<DeferredController>()

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)

    val config = ConfigLoader.load(processingEnv.filer)

    annotationProcessorContext =
      AnnotationProcessorContext(
        processingEnv.elementUtils,
        processingEnv.typeUtils,
        processingEnv.messager,
        processingEnv.filer,
        config,
      )
  }

  override fun getSupportedSourceVersion(): SourceVersion =
    SourceVersion.latestSupported()

  override fun process(
    annotations: Set<TypeElement>,
    roundEnv: RoundEnvironment
  ): Boolean {
    if (!roundEnv.processingOver()) {
      val deferredControllerContexts = getAndResetDeferredControllers()
      deferredControllerContexts.forEach(::processEachController)

      val controllerContexts = getControllers(annotations, roundEnv)
      controllerContexts.forEach(::processEachController)
    } else if (deferredControllers.isNotEmpty()) {
      outputFailedControllers()
    }

    return false
  }

  private fun outputFailedControllers() {
    deferredControllers.forEach { dc ->
      val deferredElement = annotationProcessorContext.elementUtils.getTypeElement(dc.element.qualifiedName)
      annotationProcessorContext.messages.warning("LabzenWebProcessor: 无法实现 Controller ${deferredElement.qualifiedName}, 因为无法引用类 ${dc.referencedClass}")
    }
  }

  private fun getAndResetDeferredControllers(): List<ControllerContext> {
    val readied = deferredControllers.filter {
      try {
        Class.forName(it.referencedClass)
        true
      } catch (e: Exception) {
        false
      }
    }
    val readiedContexts = readied.map {
      ControllerContext(it.element, annotationProcessorContext)
    }

    deferredControllers.removeAll(readied.toSet())

    return readiedContexts
  }

  private fun getControllers(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): List<ControllerContext> {
    return annotations.filter {
      it.kind == ElementKind.ANNOTATION_TYPE
    }.flatMap { annotation ->
      val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation).map(::asTypeElement)
      annotatedElements.map { element ->
        ControllerContext(element, annotationProcessorContext)
      }
    }
  }

  private fun processEachController(context: ControllerContext) {
    if (processedControllers.contains(context.source)) {
      println("%%%%%%%%%%%%%%  ${context.source.simpleName}")
      return
    }

    val processors = getProcessor()
    try {
      for (processor in processors) {
        processor.process(context)
      }

      processedControllers.add(context.source)
    } catch (e: TypeNotAvailableException) {
      deferredControllers.add(DeferredController(context.source, e.fqcn))
    } catch (e: Throwable) {
      handleUncaughtError(context.source, e)
    }
  }

  private fun handleUncaughtError(element: Element, thrown: Throwable) {
    val sw = StringWriter()
    thrown.printStackTrace(PrintWriter(sw))

    val reportableStacktrace = sw.toString().replace(System.lineSeparator(), "  ")

    processingEnv.messager.printMessage(
      Diagnostic.Kind.ERROR, "Internal error in the mapping processor: $reportableStacktrace", element
    )
  }

  private fun getProcessor(): List<InternalProcessor> =
    ServiceLoader.load(InternalProcessor::class.java, this.javaClass.classLoader)
      .sortedWith(processorComparator)
      .toList()

  private fun asTypeElement(element: Element): TypeElement =
    element.accept(
      object : ElementKindVisitor9<TypeElement, Void?>() {
        override fun visitTypeAsInterface(e: TypeElement, p: Void?): TypeElement {
          return e
        }

        override fun visitTypeAsClass(e: TypeElement, p: Void?): TypeElement {
          return e
        }
      }, null
    )

  data class DeferredController(val element: TypeElement, val referencedClass: String)

  companion object {
    private val processorComparator: Comparator<InternalProcessor> = Comparator.comparing(
      InternalProcessor::priority
    )
  }
}