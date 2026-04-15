package cn.labzen.web.apt.evaluate.generics;

import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.List;

import static cn.labzen.web.apt.definition.TypeNames.INTERFACE_FILE_CONTROLLER;

/**
 * 对应 cn.labzen.web.api.controller.FileController
 */
public class FileControllerInterfaceGenericsEvaluator extends PrimaryServiceGenericsAssign {

  @Override
  protected String supportedInterfaceName() {
    return INTERFACE_FILE_CONTROLLER;
  }

  @Override
  public List<? extends Suggestion> evaluate(List<TypeName> arguments) {
//    assert !arguments.isEmpty();
    return Collections.emptyList();
  }
}
