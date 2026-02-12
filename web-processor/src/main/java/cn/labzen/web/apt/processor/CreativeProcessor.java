package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.ClassCreator;
import cn.labzen.web.apt.internal.context.ControllerContext;

public final class CreativeProcessor implements InternalProcessor {

  @Override
  public void process(ControllerContext context) {
    new ClassCreator(context.getRoot(), context.getApc().filer()).create();
  }

  @Override
  public int priority() {
    return PRIORITY_CREATIVE;
  }
}
