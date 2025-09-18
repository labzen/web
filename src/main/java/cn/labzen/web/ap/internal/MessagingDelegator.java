package cn.labzen.web.ap.internal;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public record MessagingDelegator(Messager delegate) {

  public void info(String message) {
    delegate.printMessage(Diagnostic.Kind.NOTE, message);
  }

  public void warning(String message) {
    delegate.printMessage(Diagnostic.Kind.WARNING, message);
  }

  public void error(String message) {
    delegate.printMessage(Diagnostic.Kind.ERROR, message);
  }
}
