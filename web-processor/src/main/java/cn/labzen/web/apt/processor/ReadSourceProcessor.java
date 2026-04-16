package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.internal.element.ElementClass;

import javax.lang.model.type.TypeMirror;

/**
 * 读取 Controller 源码信息
 * <p>
 * 在处理流程的第二阶段执行，提取接口的类名、包名和实现类型信息，
 * 创建 ElementClass 根节点作为后续处理的入口。
 */
public final class ReadSourceProcessor implements InternalProcessor {

  /**
   * 提取并设置源码基本信息
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    String className = context.getSource().getSimpleName().toString() + context.getApc().config().classNameSuffix();
    String pkg = context.getApc().elements().getPackageOf(context.getSource()).getQualifiedName().toString();

    TypeMirror implementTypes = context.getSource().asType();

    context.setRoot(new ElementClass(className, pkg, implementTypes));
  }

  @Override
  public int priority() {
    return PRIORITY_READ_SOURCE;
  }
}
