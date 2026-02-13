package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.ClassCreator;
import cn.labzen.web.apt.internal.context.ControllerContext;

public final class CreativeProcessor implements InternalProcessor {

  @Override
  public void process(ControllerContext context) {
    try {
      new ClassCreator(context.getRoot(), context.getApc().filer()).create();
    } catch (Throwable e) {
      context.getApc().messaging().warning("CreativeProcessor: 类型检查失败，可能导致生成的代码无法编译: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public int priority() {
    return PRIORITY_CREATIVE;
  }
}
