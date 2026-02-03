package cn.labzen.web.ap.evaluate.generics;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.context.AnnotationProcessorContext;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementClass;
import cn.labzen.web.ap.internal.element.ElementField;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import static cn.labzen.web.ap.definition.TypeNames.ANNOTATION_JAKARTA_RESOURCE;

/**
 * 核心业务逻辑类泛型指向
 */
public abstract non-sealed class PrimaryServiceGenericsAssign implements InterfaceGenericsEvaluator {

  protected TypeName resourceType;
  protected TypeName supportedInterfaceType;

  @Override
  public void init(AnnotationProcessorContext context) {
    resourceType = TypeName.get(context.elements().getTypeElement(ANNOTATION_JAKARTA_RESOURCE).asType());
  }

  @Override
  public boolean support(TypeName type) {
    if (type instanceof ParameterizedTypeName parameterizedTypeName) {
      if (supportedInterfaceType instanceof ParameterizedTypeName parameterizedInterfaceType) {
        return parameterizedInterfaceType.rawType.equals(parameterizedTypeName.rawType);
      }
      return supportedInterfaceType.equals(parameterizedTypeName.rawType);
    } else {
      return supportedInterfaceType.equals(type);
    }
  }

  protected List<AppendSuggestion> internalEvaluate(TypeName primaryComponentClass) {
    String simpleName = Utils.getSimpleName(primaryComponentClass);
    String fieldName = Strings.camelCase(simpleName);
    ElementAnnotation annotation = new ElementAnnotation(resourceType);
    ElementField elementField = new ElementField(fieldName, primaryComponentClass, List.of(annotation));

    return List.of(new AppendSuggestion(elementField, ElementClass.class));
  }
}
