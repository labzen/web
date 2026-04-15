package cn.labzen.web.apt.evaluate.generics;

import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.List;

import static cn.labzen.web.apt.definition.TypeNames.INTERFACE_STANDARD_CONTROLLER;

/**
 * 对应 cn.labzen.web.controller.StandardController
 */
public class StandardControllerInterfaceGenericsEvaluator extends PrimaryServiceGenericsAssign {

  @Override
  protected String supportedInterfaceName() {
    return INTERFACE_STANDARD_CONTROLLER;
  }

  @Override
  public List<? extends Suggestion> evaluate(List<TypeName> arguments) {
    assert !arguments.isEmpty();
    return internalServiceBeanEvaluate(arguments.getFirst());
  }
}
