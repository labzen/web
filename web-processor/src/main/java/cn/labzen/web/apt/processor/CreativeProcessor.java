package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.ClassCreator;
import cn.labzen.web.apt.internal.context.ControllerContext;

/**
 * 生成 Controller 实现类的代码文件
 * <p>
 * 处理流程的最后阶段，使用 JavaPoet 库根据 ElementClass 描述生成 Java 源代码文件。
 * 生成的代码包含类定义、字段、方法和注解。
 */
public final class CreativeProcessor implements InternalProcessor {

  /**
   * 创建并输出 Java 源文件
   *
   * @param context 控制器上下文
   */
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
