package cn.labzen.web.ap.processor;

import cn.labzen.web.ap.internal.ClassCreator;
import cn.labzen.web.ap.internal.context.ControllerContext;

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
