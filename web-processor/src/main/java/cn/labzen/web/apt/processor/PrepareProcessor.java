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
 * 检查Controller接口合法性
 */
public final class PrepareProcessor implements InternalProcessor {

  @Override
  public void process(ControllerContext context) {
    TypeElement source = context.getSource();
    if (source.getKind() != ElementKind.INTERFACE) {
      context.getApc().messaging().warning("注解了 @LabzenController 的源文件必须是接口(interface)类");
    }

    if (source.getNestingKind() != NestingKind.TOP_LEVEL) {
      context.getApc().messaging().warning("注解了 @LabzenController 的接口必须是顶级类");
    }

    context.setGenericsEvaluators(getClassGenerators());
    context.setAnnotationEvaluators(getAnnotationEvaluators());
  }

  private List<InterfaceGenericsEvaluator> getClassGenerators() {
    return ServiceLoader.load(InterfaceGenericsEvaluator.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).toList();
  }

  private List<MethodAnnotationErasableEvaluator> getAnnotationEvaluators() {
    return ServiceLoader.load(MethodAnnotationErasableEvaluator.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).toList();
  }

  @Override
  public int priority() {
    return PRIORITY_PREPARE;
  }
}
