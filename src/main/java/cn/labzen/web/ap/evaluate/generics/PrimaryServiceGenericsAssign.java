package cn.labzen.web.ap.evaluate.generics;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.element.ElementAnnotation;
import cn.labzen.web.ap.internal.element.ElementClass;
import cn.labzen.web.ap.internal.element.ElementField;
import cn.labzen.web.ap.suggestion.AppendSuggestion;
import com.squareup.javapoet.TypeName;
import jakarta.annotation.Resource;

import java.util.List;

/**
 * 核心业务逻辑类泛型指向
 */
public abstract non-sealed class PrimaryServiceGenericsAssign implements InterfaceGenericsEvaluator {

  protected List<AppendSuggestion> internalEvaluate(TypeName primaryComponentClass) {
    String simpleName = Utils.getSimpleName(primaryComponentClass);
    String fieldName = Strings.camelCase(simpleName);
    ElementAnnotation annotation = new ElementAnnotation(TypeName.get(Resource.class));
    ElementField elementField = new ElementField(fieldName, primaryComponentClass, List.of(annotation));

    return List.of(new AppendSuggestion(elementField, ElementClass.class));
  }
}
