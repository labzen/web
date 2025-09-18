package cn.labzen.web.ap.internal.element;

public sealed interface Element
  permits ElementAnnotation, ElementClass, ElementField, ElementMethod, ElementMethodBody, ElementParameter {

  String keyword();
}
