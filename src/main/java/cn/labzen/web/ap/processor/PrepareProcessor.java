package cn.labzen.web.ap.processor;

import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator;
import cn.labzen.web.ap.evaluate.generics.InterfaceGenericsEvaluator;
import cn.labzen.web.ap.internal.context.ControllerContext;

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
      context.getApc().getMessaging().warning("注解了 @LabzenController 的源文件必须是接口(interface)类");
    }

    if (source.getNestingKind() != NestingKind.TOP_LEVEL) {
      context.getApc().getMessaging().warning("注解了 @LabzenController 的接口必须是顶级类");
    }

    context.setGenericsEvaluators(getClassGenerators());
    context.setAnnotationEvaluators(getAnnotationEvaluators());
  }

  private List<InterfaceGenericsEvaluator> getClassGenerators() {
    return ServiceLoader.load(InterfaceGenericsEvaluator.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).toList();
  }

  private List<MethodErasableAnnotationEvaluator> getAnnotationEvaluators() {
    return ServiceLoader.load(MethodErasableAnnotationEvaluator.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).toList();
  }

  @Override
  public int priority() {
    return PRIORITY_PREPARE;
  }
}
