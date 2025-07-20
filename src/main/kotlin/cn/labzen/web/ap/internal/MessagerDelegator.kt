package cn.labzen.web.ap.internal

import javax.annotation.processing.Messager
import javax.tools.Diagnostic

class MessagerDelegator(private val delegate: Messager) {

  fun info(message: String) {
    delegate.printMessage(Diagnostic.Kind.NOTE, message)
  }

  fun warning(message: String) {
    delegate.printMessage(Diagnostic.Kind.WARNING, message)
  }

  fun error(message: String) {
    delegate.printMessage(Diagnostic.Kind.ERROR, message)
  }

}