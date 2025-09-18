package cn.labzen.web.ap.internal.context;

import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.internal.MessagingDelegator;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * 将上下文数据保留在整个注释处理器（“应用程序范围”）的范围内。
 */
@Getter
public class AnnotationProcessorContext {

  private final Elements elements;
  private final Types types;
  private final Messager messager;
  private final Filer filer;
  private final Config config;

  @Setter
  private MessagingDelegator messaging;

  public AnnotationProcessorContext(Elements elements, Types types, Messager messager, Filer filer, Config config) {
    this.elements = elements;
    this.types = types;
    this.messager = messager;
    this.filer = filer;
    this.config = config;
  }
}
