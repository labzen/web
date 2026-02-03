package cn.labzen.web.ap.evaluate.generics;

import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;

import static cn.labzen.web.ap.definition.TypeNames.INTERFACE_STANDARD_CONTROLLER;

/**
 * 对应 cn.labzen.web.controller.StandardController
 */
public class StandardControllerInterfaceGenericsEvaluator extends PrimaryServiceGenericsAssign {

  @Override
  public void init(AnnotationProcessorContext context) {
    super.init(context);

    supportedInterfaceType = TypeName.get(context.elements().getTypeElement(INTERFACE_STANDARD_CONTROLLER).asType());
  }

  @Override
  public List<? extends Suggestion> evaluate(List<TypeName> arguments) {
    assert !arguments.isEmpty();
    return internalEvaluate(arguments.getFirst());
  }
}
