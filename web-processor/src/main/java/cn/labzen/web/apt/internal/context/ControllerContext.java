package cn.labzen.web.apt.internal.context;

import cn.labzen.web.apt.LabzenWebProcessor;
import cn.labzen.web.apt.evaluate.annotation.MethodAnnotationErasableEvaluator;
import cn.labzen.web.apt.evaluate.generics.InterfaceGenericsEvaluator;
import cn.labzen.web.apt.internal.element.ElementClass;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static cn.labzen.web.apt.definition.TypeNames.INTERFACE_BASE_CONTROLLER;

@Getter
@Setter
public class ControllerContext {

  private final TypeElement source;
  private final TypeMirror ancestorControllerType;

  private List<InterfaceGenericsEvaluator> genericsEvaluators;
  private List<MethodAnnotationErasableEvaluator> annotationEvaluators;
  private ElementClass root;

  public ControllerContext(TypeElement source) {
    this.source = source;

    ancestorControllerType = LabzenWebProcessor.getContext().elements().getTypeElement(INTERFACE_BASE_CONTROLLER).asType();
  }
  
  public AnnotationProcessorContext getApc() {
    return LabzenWebProcessor.getContext();
  }
}
