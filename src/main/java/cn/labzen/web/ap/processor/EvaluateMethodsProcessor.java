package cn.labzen.web.ap.processor;

import cn.labzen.tool.util.Collections;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.ap.config.Config;
import cn.labzen.web.ap.evaluate.annotation.MethodErasableAnnotationEvaluator;
import cn.labzen.web.ap.internal.Utils;
import cn.labzen.web.ap.internal.context.ControllerContext;
import cn.labzen.web.ap.internal.element.*;
import cn.labzen.web.ap.suggestion.*;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public final class EvaluateMethodsProcessor implements InternalProcessor {

  private static final Set<TypeName> RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD = Set.of(
    TypeName.get(Override.class),
    TypeName.get(Nonnull.class),
    TypeName.get(Nullable.class));

  private final Map<String, ElementMethod> parsedMethods = new ConcurrentHashMap<>();

  private Types types;
  private DeclaredType sourceType;
  private ElementClass elementClass;
  private Config config;
  private List<MethodErasableAnnotationEvaluator> evaluators;

  @Override
  public void process(ControllerContext context) {
    this.types = context.getApc().getTypes();
    this.sourceType = (DeclaredType) context.getSource().asType();
    this.elementClass = context.getRoot();
    this.config = context.getApc().getConfig();
    this.evaluators = context.getAnnotationEvaluators();

    collectMethods(context, context.getSource());

    parsedMethods.forEach((key, method) -> {
      elementClass.getMethods().add(method);
    });
  }

  private void collectMethods(ControllerContext context, TypeElement source) {
    List<? extends TypeMirror> directSupertypes = types.directSupertypes(source.asType());
    directSupertypes.forEach(superType -> {
      // 如果controller的父类型不是LabzenController及其子接口，则不处理
      boolean supportInterface = types.isAssignable(superType, context.getAncestorControllerType());
      if (!supportInterface) {
        return;
      }

      TypeElement superElement = (TypeElement) ((DeclaredType) superType).asElement();
      collectMethods(context, superElement);
    });

    ElementFilter.methodsIn(source.getEnclosedElements()).forEach(this::parseMethod);
  }

  private void parseMethod(ExecutableElement method) {
    String methodName = method.getSimpleName().toString();
    TypeName returnType = Utils.typeOf(method.getReturnType());

    ExecutableType resolvedMethodType = (ExecutableType) types.asMemberOf(sourceType, method);
    // 真实的方法参数类型
    List<? extends TypeMirror> actualParameterTypes = resolvedMethodType.getParameterTypes();
    List<String> parameterTypeNames = actualParameterTypes.stream().map(pt -> {
      TypeName type = Utils.typeOf(pt);
      return Utils.getSimpleName(type);
    }).toList();
    String parametersSignature = String.join(", ", parameterTypeNames);

    List<String> parameterNames = method.getParameters().stream()
      .map(param -> param.getSimpleName().toString()).toList();
    String methodSignature = Utils.getSimpleName(returnType) + " " + methodName + "(" + parametersSignature + ")";

    ElementMethod elementMethod = parsedMethods.computeIfAbsent(methodSignature, key -> {
      ElementMethod em = new ElementMethod(methodName, returnType);
      em.setBody(new ElementMethodBody("", methodName, parameterNames));
      return em;
    });

    // 读取所有参数（及注解）
    List<ElementParameter> methodParameters = readParameters(method.getParameters(), actualParameterTypes);
    methodParameters.forEach(parameter -> {
      boolean superParameterExists = elementMethod.getParameters().contains(parameter);
      if (!superParameterExists) {
        elementMethod.getParameters().add(parameter);
        return;
      }

      // 重写的方法，参数列表需要覆盖掉父接口中定义的注解集合
      Optional<ElementParameter> found = elementMethod.getParameters().stream()
        .filter(ep -> ep.equals(parameter)).findFirst();
      found.ifPresent(ep -> ep.getAnnotations().addAll(parameter.getAnnotations()));
    });
    elementMethod.getParameters().addAll(methodParameters);

    // 读取所有的方法注解
    List<ElementAnnotation> methodAnnotations = method.getAnnotationMirrors().stream().filter(annotationMirror -> {
      // 过滤掉 jetbrains 的注解，主要是 NotNull 和 Nullable，强制使用 JSR305 的 javax.annotation 下的注解
      String fqcn = annotationMirror.getAnnotationType().asElement().toString();
      return !fqcn.startsWith("org.jetbrains.annotations.");
    }).map(annotationMirror -> {
      TypeName annotationType = Utils.typeOf(annotationMirror.getAnnotationType().asElement().asType());
      Map<String, Object> annotationMembers = Utils.readAnnotationMembers(annotationMirror);

      return new ElementAnnotation(annotationType, annotationMembers);
    }).toList();
    elementMethod.getAnnotations().addAll(methodAnnotations);

    parseMethodAnnotations(elementMethod);
  }

  private List<ElementParameter> readParameters(List<? extends VariableElement> parameterElements, List<? extends TypeMirror> actualParameterTypes) {
    List<ElementParameter> parameters = new ArrayList<>();
    int index = 0;

    for (VariableElement parameterElement : parameterElements) {
      String parameterName = parameterElement.getSimpleName().toString();
      TypeName parameterType = Utils.typeOf(actualParameterTypes.get(index));

      LinkedHashSet<ElementAnnotation> annotations = readAnnotations(parameterElement.getAnnotationMirrors());
      ElementParameter parameter = new ElementParameter(index, parameterName, parameterType, annotations);
      parameters.add(parameter);
      index++;
    }

    return parameters;
  }

  private LinkedHashSet<ElementAnnotation> readAnnotations(List<? extends AnnotationMirror> annotationMirrors) {
    LinkedHashSet<ElementAnnotation> annotations = new LinkedHashSet<>();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      ClassName annotationClass = Utils.classOf(annotationMirror.getAnnotationType().asElement());
      Map<String, Object> annotationMembers = Utils.readAnnotationMembers(annotationMirror);

      ElementAnnotation annotation = new ElementAnnotation(annotationClass, annotationMembers);
      annotations.add(annotation);
    }

    return annotations;
  }

  private void parseMethodAnnotations(ElementMethod method) {
    List<? extends Suggestion> suggestions = method.getAnnotations().stream()
      .flatMap(annotation ->
        evaluators.stream().flatMap(evaluator -> {
          TypeName type = annotation.getType();
          if (evaluator.support(type)) {
            return evaluator.evaluate(config, type, annotation.getMembers()).stream();
          } else {
            return Stream.of();
          }
        })).toList();

    suggestions.forEach(suggestion -> {
      switch (suggestion) {
        case AppendSuggestion append -> parseAppendSuggestion(method, append);
        case RemoveSuggestion remove -> parseRemoveSuggestion(method, remove);
        case ReplaceSuggestion replace -> parseReplaceSuggestion(method, replace);
        case DiscardSuggestion ignored -> parseDiscardSuggestion(method);
      }
    });
  }

  private void parseAppendSuggestion(ElementMethod method, AppendSuggestion suggestion) {
    if (suggestion.element() instanceof ElementField elementField) {
      elementClass.getFields().add(elementField);
    } else if (suggestion.element() instanceof ElementAnnotation elementAnnotation) {
      if (ElementClass.class.equals(suggestion.kind())) {
        elementClass.getAnnotations().add(elementAnnotation);
      } else if (ElementMethod.class.equals(suggestion.kind())) {
        method.getAnnotations().add(elementAnnotation);
      }
    }
  }

  private void parseRemoveSuggestion(ElementMethod method, RemoveSuggestion suggestion) {
    if (ElementClass.class.equals(suggestion.kind())) {
      removeNeedlessElements(elementClass.getFields(), field -> field.keyword().equals(suggestion.keyword()));

      removeNeedlessElements(elementClass.getAnnotations(), annotation -> annotation.keyword().equals(suggestion.keyword()));
    } else if (ElementMethod.class.equals(suggestion.kind())) {
      removeNeedlessElements(method.getAnnotations(), annotation -> annotation.keyword().equals(suggestion.keyword()));
    }
  }

  private void parseReplaceSuggestion(ElementMethod method, ReplaceSuggestion suggestion) {
    if (suggestion.element() instanceof ElementMethodBody elementMethodBody) {
      String fieldName = Strings.valueWhenBlank(elementMethodBody.getFieldName(), method.getBody().getFieldName());
      String invokeMethodName = Strings.valueWhenBlank(elementMethodBody.getInvokeMethodName(), method.getBody().getInvokeMethodName());
      List<String> parameterNames = elementMethodBody.getParameterNames();
      if (Collections.isNullOrEmpty(parameterNames)) {
        parameterNames = method.getBody().getParameterNames();
      }

      method.setBody(new ElementMethodBody(fieldName, invokeMethodName, parameterNames));
    } else if (suggestion.element() instanceof ElementAnnotation elementAnnotation) {
      Optional<ElementAnnotation> found = method.getAnnotations().stream()
        .filter(annotation -> annotation.keyword().equals(suggestion.keyword())).findFirst();
      found.ifPresent(annotation -> annotation.getMembers().putAll(elementAnnotation.getMembers()));
    }
  }

  private void parseDiscardSuggestion(ElementMethod method) {
    // 移除方法上的注解，忽略Override, Nonnull等
    removeNeedlessElements(method.getAnnotations(), annotation -> !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(((ElementAnnotation) annotation).getType()));

    method.getParameters().forEach(parameter -> {
      // 移除方法参数的注解，忽略Override, Nonnull等
      removeNeedlessElements(method.getAnnotations(), annotation -> !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(((ElementAnnotation) annotation).getType()));
    });

    method.setBody(new ElementMethodBody("", "", java.util.Collections.emptyList()));
  }

  private void removeNeedlessElements(LinkedHashSet<? extends Element> elements, Function<Element, Boolean> condition) {
    List<? extends Element> needlessElements = elements.stream().filter(condition::apply).toList();
    for (Element element : needlessElements) {
      elements.remove(element);
    }
  }

  @Override
  public int priority() {
    return PRIORITY_EVALUATE_METHODS;
  }
}
