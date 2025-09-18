package cn.labzen.web.ap.evaluate.generics;

import cn.labzen.web.ap.suggestion.Suggestion;
import cn.labzen.web.controller.StandardController;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

public class StandardControllerInterfaceGenericsEvaluator extends PrimaryServiceGenericsAssign {

  private static final TypeName TYPE = TypeName.get(StandardController.class);

  @Override
  public boolean support(TypeName type) {
    if (type instanceof ParameterizedTypeName parameterizedTypeName) {
      return TYPE.equals(parameterizedTypeName.rawType);
    } else {
      return TYPE.equals(type);
    }
  }

  @Override
  public List<? extends Suggestion> evaluate(List<TypeName> arguments) {
    assert !arguments.isEmpty();
    return internalEvaluate(arguments.getFirst());
  }
}
