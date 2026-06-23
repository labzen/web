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
  private volatile List<InternalProcessor> cachedProcessors;

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
    } else {
      if (!deferredControllers.isEmpty()) {
        outputFailedControllers();
      }
      // 清理本轮编译的累积状态，为下一次增量编译做准备
      // Eclipse JDT APT 会复用处理器实例，不清理会导致增量编译时跳过已修改的 Controller
      processedControllers.clear();
      deferredControllers.clear();
      // 清理缓存的处理器列表，确保下一次编译（尤其是 ECJ 增量编译复用处理器实例时）
      // 使用全新的 InternalProcessor 实例，避免跨编译轮次的残留状态
      cachedProcessors = null;
    }

    return true;
  }

  /**
   * 输出编译结束时仍未成功处理的控制器错误
   * <p>
   * 此时控制器已耗尽所有重试机会，属于真正的编译错误，使用 ERROR 级别
   */
  private void outputFailedControllers() {
    deferredControllers.forEach(deferred -> {
      TypeElement deferredElement = getContext().elements()
        .getTypeElement(deferred.element().getQualifiedName());
      getContext().messaging().error("LabzenWebProcessor: 无法实现 Controller " + deferredElement.getQualifiedName());
    });
  }

  /**
   * 获取并重置上一轮延迟的控制器上下文列表
   * <p>
   * 只重试尚未耗尽重试次数的控制器
   *
   * @return 延迟控制器的上下文列表
   */
  private List<ControllerContext> getAndResetDeferredControllers() {
    List<ControllerContext> result = deferredControllers.stream()
      .filter(DeferredController::canRetry)
      .map(controller -> new ControllerContext(controller.element())).toList();
    // 仅移除可重试的控制器，不可重试的保留到 processingOver 阶段报告错误
    deferredControllers.removeIf(DeferredController::canRetry);
    return result;
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
      // 延迟处理：将控制器加入重试队列，使用 WARNING 而非 ERROR
      // 关键：如果在此处报告 ERROR，ECJ（Eclipse 编译器）会进入"proceed on error"模式，
      // 导致同一编译批次中其他无关类（如使用跨模块 static import 的类）也无法解析依赖，
      // 最终生成 throw new Error("Unresolved compilation problem: ...") 的占位字节码。
      // 改为 WARNING 后，控制器仍会被延迟重试，只有重试耗尽后才在 outputFailedControllers 中报告 ERROR。
      DeferredController existing = findDeferredController(context.getSource());
      DeferredController deferred = existing != null ? existing.incrementRetry() : new DeferredController(context.getSource());
      deferredControllers.remove(existing);
      deferredControllers.add(deferred);
      handleUncaughtError(context.getSource(), e, deferred.canRetry());
      getContext().messaging().info("Labzen web processor -     process failed controller: " + context.getSource().getQualifiedName());
    }
  }

  /**
   * 在延迟队列中查找指定的控制器
   */
  private DeferredController findDeferredController(TypeElement source) {
    return deferredControllers.stream()
      .filter(dc -> dc.element().equals(source))
      .findFirst()
      .orElse(null);
  }

  /**
   * 处理未捕获的异常，将其转换为编译信息输出
   * <p>
   * 当 canRetry=true 时使用 WARNING 级别（控制器将被延迟重试，非致命错误）；
   * 当 canRetry=false 时使用 ERROR 级别（重试已耗尽，属于真正的编译错误）。
   *
   * @param element  出错的元素
   * @param e        异常对象
   * @param canRetry 是否还可以重试
   */
  private void handleUncaughtError(Element element, Throwable e, boolean canRetry) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));

    String reportableStacktrace = sw.toString().replace(System.lineSeparator(), "  ");
    String message = "Labzen web processor - Internal error in the mapping processor: " + reportableStacktrace;

    if (canRetry) {
      getContext().messaging().delegate().printMessage(Diagnostic.Kind.WARNING, message, element);
    } else {
      getContext().messaging().delegate().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
  }

  /**
   * 通过 SPI 加载所有内部处理器，并按优先级排序
   * <p>
   * 结果缓存在实例字段中，避免每次处理 Controller 时重复调用 ServiceLoader。
   *
   * @return 已排序的处理器列表
   */
  private List<InternalProcessor> getProcessors() {
    if (cachedProcessors == null) {
      synchronized (this) {
        if (cachedProcessors == null) {
          ClassLoader classLoader = this.getClass().getClassLoader();
          cachedProcessors = ServiceLoader.load(InternalProcessor.class, classLoader).stream()
            .map(ServiceLoader.Provider::get)
            .sorted(PROCESSOR_COMPARATOR)
            .toList();
        }
      }
    }
    return cachedProcessors;
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
