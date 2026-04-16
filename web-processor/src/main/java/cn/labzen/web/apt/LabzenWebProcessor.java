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

/**
 * Labzen Web 注解处理器
 * <p>
 * 负责扫描所有标注了 @LabzenController 注解的接口，并生成对应的 Controller 实现类。
 * 处理流程分为多个阶段：准备、读取源码、读取注解、评价字段、评价方法、生成代码。
 */
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes(APT_ANNOTATION_LABZEN_CONTROLLER)
// 真邪了门儿了，最朴实的方式javax.annotation.processing.Processor文件，编译会报错，用这个注解自动生成SPI文件就没事儿，MLGB！
@AutoService(Processor.class)
public class LabzenWebProcessor extends AbstractProcessor {

  private static final Comparator<InternalProcessor> PROCESSOR_COMPARATOR = Comparator
    .comparing(InternalProcessor::priority);

  // 全局静态可访问的 AnnotationProcessorContext，使用 ThreadLocal 确保线程安全
  private static final ThreadLocal<AnnotationProcessorContext> CONTEXT_HOLDER = new ThreadLocal<>();

  /**
   * 获取当前线程的 AnnotationProcessorContext
   *
   * @return 当前线程的注解处理器上下文
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

  /**
   * 处理注解的主入口
   * <p>
   * 核心处理逻辑：
   * <ul>
   *   <li>1. 先处理上一轮延迟的控制器（可能是因为依赖项尚未编译）</li>
   *   <li>2. 处理当前轮次发现的控制器</li>
   *   <li>3. 编译结束时输出失败的控制警告</li>
   * </ul>
   *
   * @param annotations 被注解的类型元素集合
   * @param roundEnv    当前编译轮次的环境
   * @return 是否声明已处理该注解，true 表示该注解已被此处理器处理
   */
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

    return true;
  }

  /**
   * 输出编译结束时仍未成功处理的控制器警告
   */
  private void outputFailedControllers() {
    deferredControllers.forEach(deferred -> {
      TypeElement deferredElement = getContext().elements()
        .getTypeElement(deferred.element().getQualifiedName());
      getContext().messaging()
        .warning("LabzenWebProcessor: 无法实现 Controller " + deferredElement.getQualifiedName());
    });
  }

  /**
   * 获取并重置上一轮延迟的控制器上下文列表
   *
   * @return 延迟控制器的上下文列表
   */
  private List<ControllerContext> getAndResetDeferredControllers() {
    return deferredControllers.stream()
      .map(controller -> new ControllerContext(controller.element())).toList();
  }

  /**
   * 从注解和环境获取所有需要处理的控制器接口
   *
   * @param annotations 注解类型元素集合
   * @param roundEnv    编译轮次环境
   * @return 控制器上下文列表
   */
  private List<ControllerContext> getControllers(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return annotations.stream()
      .filter(annotation -> annotation.getKind() == ElementKind.ANNOTATION_TYPE)
      .flatMap(annotation -> roundEnv.getElementsAnnotatedWith(annotation).stream()
        .map(this::asTypeElement)
        .map(ControllerContext::new))
      .toList();
  }

  /**
   * 处理单个控制器的完整流程
   * <p>
   * 依次执行所有处理器，最后输出成功或失败日志。
   * 如果处理过程中发生异常，会将控制器添加到延迟列表中。
   *
   * @param context 控制器上下文
   */
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

  /**
   * 处理未捕获的异常，将其转换为编译错误信息输出
   *
   * @param element 出错的元素
   * @param e       异常对象
   */
  private void handleUncaughtError(Element element, Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));

    String reportableStacktrace = sw.toString().replace(System.lineSeparator(), "  ");

    getContext().messaging().delegate().printMessage(Diagnostic.Kind.ERROR,
      "Labzen web processor - Internal error in the mapping processor: " + reportableStacktrace, element);
  }

  /**
   * 通过 SPI 加载所有内部处理器，并按优先级排序
   *
   * @return 已排序的处理器列表
   */
  private List<InternalProcessor> getProcessors() {
    // 使用当前线程的上下文类加载器，提高兼容性
    ClassLoader classLoader = this.getClass().getClassLoader();
    return ServiceLoader.load(InternalProcessor.class, classLoader).stream().map(ServiceLoader.Provider::get)
      .sorted(PROCESSOR_COMPARATOR).toList();
  }

  /**
   * 将 Element 转换为 TypeElement
   * <p>
   * 使用 Visitor 模式分别处理类和接口类型的元素。
   *
   * @param element 要转换的元素
   * @return 转换后的类型元素
   */
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
