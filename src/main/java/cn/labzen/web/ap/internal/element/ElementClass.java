package cn.labzen.web.ap.internal.element;

import lombok.Getter;

import javax.lang.model.type.TypeMirror;
import java.util.LinkedHashSet;

@Getter
public final class ElementClass implements Element {

  private final String name;
  private final String pkg;
  private final TypeMirror implementTypes;

  private final LinkedHashSet<ElementAnnotation> annotations = new LinkedHashSet<>();
  private final LinkedHashSet<ElementField> fields = new LinkedHashSet<>();
  private final LinkedHashSet<ElementMethod> methods = new LinkedHashSet<>();

  public ElementClass(String name, String pkg, TypeMirror implementTypes) {
    this.name = name;
    this.pkg = pkg;
    this.implementTypes = implementTypes;
  }

  public void addAnnotation(ElementAnnotation annotation) {
    annotations.add(annotation);
  }

  public void addField(ElementField field) {
    fields.add(field);
  }

  public void addMethod(ElementMethod method) {
    methods.add(method);
  }

  @Override
  public String keyword() {
    return pkg + "." + name;
  }
}
