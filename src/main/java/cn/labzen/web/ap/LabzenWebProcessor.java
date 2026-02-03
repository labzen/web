package cn.labzen.web.ap;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.config.ConfigLoader;
import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.internal.context.ControllerContext;
import cn.labzen.web.ap.processor.InternalProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementKindVisitor14;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes("cn.labzen.web.annotation.LabzenController")
@AutoService(Processor.class) // 真邪了门儿了，最朴实的方式javax.annotation.processing.Processor文件，编译会报错，用这个注解自动生成SPI文件就没事儿，MLGB！
public class LabzenWebProcessor extends AbstractProcessor {

  private static final Comparator<InternalProcessor> PROCESSOR_COMPARATOR = Comparator.comparing(InternalProcessor::priority);

  private final Set<TypeElement> processedControllers = Sets.newConcurrentHashSet();
  private final Set<DeferredController> deferredControllers = Sets.newConcurrentHashSet();

  private AnnotationProcessorContext annotationProcessorContext;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    Config config = ConfigLoader.load(processingEnv.getFiler());

    this.annotationProcessorContext = new AnnotationProcessorContext(processingEnv.getElementUtils(), processingEnv.getTypeUtils(), processingEnv.getMessager(), processingEnv.getFiler(), config);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!roundEnv.processingOver()) {
      List<ControllerContext> deferredControllerContexts = getAndResetDeferredControllers();
      deferredControllerContexts.forEach(this::processEachController);

      List<ControllerContext> controllerContexts = getControllers(annotations, roundEnv);
      controllerContexts.forEach(this::processEachController);
    } else if (!deferredControllers.isEmpty()) {
      outputFailedControllers();
    }

    return false;
  }

  private void outputFailedControllers() {
    deferredControllers.forEach(deferred -> {
      TypeElement deferredElement = annotationProcessorContext.elements().getTypeElement(deferred.element().getQualifiedName());
      annotationProcessorContext.messaging().warning("LabzenWebProcessor: 无法实现 Controller " + deferredElement.getQualifiedName());
    });
  }

  private List<ControllerContext> getAndResetDeferredControllers() {
    return deferredControllers.stream().map(controller -> new ControllerContext(controller.element(), annotationProcessorContext)).toList();
  }

  private List<ControllerContext> getControllers(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return annotations.stream()
      .filter(annotation -> annotation.getKind() == ElementKind.ANNOTATION_TYPE)
      .flatMap(annotation ->
        roundEnv.getElementsAnnotatedWith(annotation).stream()
          .map(this::asTypeElement)
          .map(element -> new ControllerContext(element, annotationProcessorContext))
      ).toList();
  }

  private void processEachController(ControllerContext context) {
    if (processedControllers.contains(context.getSource())) {
      return;
    }

    List<InternalProcessor> processors = getProcessors();
    try {
      for (InternalProcessor processor : processors) {
        processor.process(context);
      }

      processedControllers.add(context.getSource());
    } catch (Throwable e) {
      deferredControllers.add(new DeferredController(context.getSource()));
      handleUncaughtError(context.getSource(), e);
    }
  }

  private void handleUncaughtError(Element element, Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));

    String reportableStacktrace = sw.toString().replace(System.lineSeparator(), "  ");

    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Internal error in the mapping processor: " + reportableStacktrace, element);
  }

  private List<InternalProcessor> getProcessors() {
    return ServiceLoader.load(InternalProcessor.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).sorted(PROCESSOR_COMPARATOR).toList();
  }

  private TypeElement asTypeElement(Element element) {
    return element.accept(new ElementKindVisitor14<TypeElement, Void>() {
      @Override
      public TypeElement visitTypeAsClass(TypeElement e, Void unused) {
        return e;
      }

      @Override
      public TypeElement visitTypeAsInterface(TypeElement e, Void unused) {
        return e;
      }
    }, null);
  }
}
