package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.evaluate.annotation.MethodAnnotationErasableEvaluator;
import cn.labzen.web.apt.evaluate.generics.InterfaceGenericsEvaluator;
import cn.labzen.web.apt.internal.context.ControllerContext;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Controller 接口合法性检查处理器
 * <p>
 * 在处理流程的最早阶段执行，负责验证被 @LabzenController 注解的接口是否符合规范，
 * 并初始化所有后续处理所需的评价器。
 */
public final class PrepareProcessor implements InternalProcessor {

  /**
   * 执行合法性检查和评价器初始化
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    TypeElement source = context.getSource();
    if (source.getKind() != ElementKind.INTERFACE) {
      context.getApc().messaging().warning("注解了 @LabzenController 的源文件必须是接口(interface)类");
    }

    if (source.getNestingKind() != NestingKind.TOP_LEVEL) {
      context.getApc().messaging().warning("注解了 @LabzenController 的接口必须是顶级类");
    }

    context.setGenericsEvaluators(getClassGenerators(context));
    context.setAnnotationEvaluators(getAnnotationEvaluators(context));
  }

  /**
   * 加载并初始化所有泛型评价器
   *
   * @param context 注解处理器上下文
   * @return 已初始化的评价器列表
   */
  private List<InterfaceGenericsEvaluator> getClassGenerators(ControllerContext context) {
    return ServiceLoader.load(InterfaceGenericsEvaluator.class, this.getClass().getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .peek(evaluator -> evaluator.init(context.getApc()))
      .toList();
  }

  /**
   * 加载并初始化所有注解评价器
   *
   * @param context 注解处理器上下文
   * @return 已初始化的评价器列表
   */
  private List<MethodAnnotationErasableEvaluator> getAnnotationEvaluators(ControllerContext context) {
    return ServiceLoader.load(MethodAnnotationErasableEvaluator.class, this.getClass().getClassLoader())
      .stream()
      .map(ServiceLoader.Provider::get)
      .peek(evaluator -> evaluator.init(context.getApc()))
      .toList();
  }

  @Override
  public int priority() {
    return PRIORITY_PREPARE;
  }
}
