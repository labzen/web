package cn.labzen.web.ap.internal.context

import cn.labzen.web.ap.config.Config
import cn.labzen.web.ap.internal.MessagerDelegator
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * 将上下文数据保留在整个注释处理器（“应用程序范围”）的范围内。
 */
data class AnnotationProcessorContext(
  val elementUtils: Elements,
  val typeUtils: Types,
  val messager: Messager,
  val filer: Filer,
  val config: Config
) {

  val messages = MessagerDelegator(messager)
}