package cn.labzen.web.ap.processor;

import cn.labzen.web.ap.internal.context.ControllerContext;

public sealed interface InternalProcessor permits CreativeProcessor, EvaluateFieldsProcessor, EvaluateMethodsProcessor, PrepareProcessor, ReadAnnotationsProcessor, ReadSourceProcessor {

  int PRIORITY_PREPARE = 1;
  int PRIORITY_READ_SOURCE = 2;
  int PRIORITY_READ_ANNOTATION = 3;
  int PRIORITY_EVALUATE_FIELDS = 4;
  int PRIORITY_EVALUATE_METHODS = 5;
  int PRIORITY_CREATIVE = 6;

  void process(ControllerContext context);

  /**
   * 处理器的优先级
   * <p>
   * 该值最高优先级为最小的数字，最低优先级为最大数字；按照最高到最低的顺序调用处理器
   */
  int priority();
}
