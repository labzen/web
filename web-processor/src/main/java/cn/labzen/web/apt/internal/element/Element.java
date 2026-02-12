package cn.labzen.web.apt.internal.element;

public sealed interface Element
  permits ElementAnnotation, ElementClass, ElementField, ElementMethod, ElementMethodBody, ElementParameter {

  String keyword();
}
