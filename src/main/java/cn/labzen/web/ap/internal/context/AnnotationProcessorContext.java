package cn.labzen.web.ap.internal.context;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.MessagingDelegator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * 将上下文数据保留在整个注释处理器（“应用程序范围”）的范围内。
 */
public record AnnotationProcessorContext(Elements elements, Types types, MessagingDelegator messaging, Filer filer,
                                         Config config) {

  public AnnotationProcessorContext(Elements elements, Types types, Messager messaging, Filer filer, Config config) {
    this(elements, types, new MessagingDelegator(messaging), filer, config);
  }
}
