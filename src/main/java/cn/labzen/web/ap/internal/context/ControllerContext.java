package cn.labzen.web.ap.internal.context;

import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator;
import cn.labzen.web.ap.evaluate.generics.InterfaceGenericsEvaluator;
import cn.labzen.web.ap.internal.element.ElementClass;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

@Getter
@Setter
public class ControllerContext {

  private static final String ANCESTOR_NAME = "cn.labzen.web.controller.LabzenController";

  private final TypeElement source;
  private final AnnotationProcessorContext apc;
  private final TypeMirror ancestorControllerType;

  private List<InterfaceGenericsEvaluator> genericsEvaluators;
  private List<MethodErasableAnnotationEvaluator> annotationEvaluators;
  private ElementClass root;

  public ControllerContext(TypeElement source, AnnotationProcessorContext apc) {
    this.source = source;
    this.apc = apc;

    ancestorControllerType = apc.getElements().getTypeElement(ANCESTOR_NAME).asType();
  }

}
