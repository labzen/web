package cn.labzen.web.apt;

import cn.labzen.web.apt.config.Config;
import cn.labzen.web.apt.config.ConfigLoader;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.processor.InternalProcessor;
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

import static cn.labzen.web.apt.definition.TypeNames.APT_ANNOTATION_LABZEN_CONTROLLER;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes(APT_ANNOTATION_LABZEN_CONTROLLER)
@AutoService(Processor.class) // 真邪了门儿了，最朴实的方式javax.annotation.processing.Processor文件，编译会报错，用这个注解自动生成SPI文件就没事儿，MLGB！
public class LabzenWebProcessor extends AbstractProcessor {

  private static final Comparator<InternalProcessor> PROCESSOR_COMPARATOR = Comparator
    .comparing(InternalProcessor::priority);

  // 全局静态可访问的 AnnotationProcessorContext，使用 ThreadLocal 确保线程安全
  private static final ThreadLocal<AnnotationProcessorContext> CONTEXT_HOLDER = new ThreadLocal<>();

  /**
   * 获取当前线程的 AnnotationProcessorContext
   */
  public static AnnotationProcessorContext getContext() {
    return CONTEXT_HOLDER.get();
  }

  private final Set<TypeElement> processedControllers = Sets.newConcurrentHashSet();
  private final Set<DeferredController> deferredControllers = Sets.newConcurrentHashSet();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    Config config = ConfigLoader.load(processingEnv.getFiler());

    AnnotationProcessorContext context = new AnnotationProcessorContext(processingEnv.getElementUtils(),
      processingEnv.getTypeUtils(), processingEnv.getMessager(), processingEnv.getFiler(), config);

    // 设置全局静态访问的上下文
    CONTEXT_HOLDER.set(context);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!roundEnv.processingOver()) {
      // getContext().messaging().info("Labzen web processor is running.");

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
      TypeElement deferredElement = getContext().elements()
        .getTypeElement(deferred.element().getQualifiedName());
      getContext().messaging()
        .warning("LabzenWebProcessor: 无法实现 Controller " + deferredElement.getQualifiedName());
    });
  }

  private List<ControllerContext> getAndResetDeferredControllers() {
    return deferredControllers.stream()
      .map(controller -> new ControllerContext(controller.element())).toList();
  }

  private List<ControllerContext> getControllers(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return annotations.stream()
      .filter(annotation -> annotation.getKind() == ElementKind.ANNOTATION_TYPE)
      .flatMap(annotation -> roundEnv.getElementsAnnotatedWith(annotation).stream()
        .map(this::asTypeElement)
        .map(ControllerContext::new))
      .toList();
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
      getContext().messaging().info("Labzen web processor -     process success controller: " + context.getSource().getQualifiedName());
    } catch (Throwable e) {
      deferredControllers.add(new DeferredController(context.getSource()));
      handleUncaughtError(context.getSource(), e);
      getContext().messaging().info("Labzen web processor -     process failed controller: " + context.getSource().getQualifiedName());
    }
  }

  private void handleUncaughtError(Element element, Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));

    String reportableStacktrace = sw.toString().replace(System.lineSeparator(), "  ");

    getContext().messaging().delegate().printMessage(Diagnostic.Kind.ERROR,
      "Labzen web processor - Internal error in the mapping processor: " + reportableStacktrace, element);
  }

  private List<InternalProcessor> getProcessors() {
    // 使用当前线程的上下文类加载器，提高兼容性
    ClassLoader classLoader = this.getClass().getClassLoader();
    return ServiceLoader.load(InternalProcessor.class, classLoader).stream().map(ServiceLoader.Provider::get)
      .sorted(PROCESSOR_COMPARATOR).toList();
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
