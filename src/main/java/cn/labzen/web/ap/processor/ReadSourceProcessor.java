package cn.labzen.web.ap.processor;

import cn.labzen.web.ap.internal.context.ControllerContext;
import cn.labzen.web.ap.internal.element.ElementClass;

import javax.lang.model.type.TypeMirror;

public final class ReadSourceProcessor implements InternalProcessor {

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
