package cn.labzen.web.apt.processor;

import cn.labzen.tool.util.Collections;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.evaluate.annotation.MethodAnnotationErasableEvaluator;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.internal.element.*;
import cn.labzen.web.apt.suggestion.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 评价 Controller 接口的方法
 * <p>
 * 核心处理逻辑：
 * <ul>
 *   <li>递归收集所有继承自 LabzenController 体系的接口方法</li>
 *   <li>解析方法参数、返回值类型、注解信息</li>
 *   <li>根据方法注解生成代码生成建议</li>
 *   <li>处理 @Abandoned 等特殊注解，标记废弃方法</li>
 * </ul>
 */
public final class EvaluateMethodsProcessor implements InternalProcessor {

  /**
   * 废弃方法时需要保留的注解集合
   */
  private static final Set<TypeName> RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD = Set.of(
    TypeName.get(Override.class),
    TypeName.get(Nonnull.class),
    TypeName.get(Nullable.class));

  private final Map<String, ElementMethod> parsedMethods = new ConcurrentHashMap<>();

  private ControllerContext context;
  private DeclaredType sourceType;
  private ElementClass elementClass;
  private List<MethodAnnotationErasableEvaluator> evaluators;

  /**
   * 处理接口方法，生成方法元素
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    this.context = context;
    this.sourceType = (DeclaredType) context.getSource().asType();
    this.elementClass = context.getRoot();
    this.evaluators = context.getAnnotationEvaluators();

    collectMethods(context.getSource());

    parsedMethods.forEach((key, method) -> {
      elementClass.getMethods().add(method);
    });
  }

  /**
   * 递归收集接口及其父接口中定义的方法
   * <p>
   * 只处理继承自 LabzenController 体系的接口，忽略其他无关接口。
   *
   * @param source 接口类型元素
   */
  private void collectMethods(TypeElement source) {
    List<? extends TypeMirror> directSupertypes = context.getApc().types().directSupertypes(source.asType());
    directSupertypes.forEach(superType -> {
      // 如果controller的父类型不是LabzenController及其子接口，则不处理
      boolean supportInterface = context.getApc().types().isAssignable(superType, context.getAncestorControllerType());
      if (!supportInterface) {
        return;
      }

      TypeElement superElement = (TypeElement) ((DeclaredType) superType).asElement();
      collectMethods(superElement);
    });

    ElementFilter.methodsIn(source.getEnclosedElements()).forEach(this::parseMethod);
  }

  /**
   * 解析单个方法的完整信息
   * <p>
   * 核心逻辑：
   * <ul>
   *   <li>1. 提取方法签名、返回类型、参数信息</li>
   *   <li>2. 检查参数名是否被编译器优化为 argN 形式</li>
   *   <li>3. 处理方法注解并生成代码建议</li>
   * </ul>
   *
   * @param method 方法元素
   */
  private void parseMethod(ExecutableElement method) {
    String methodName = method.getSimpleName().toString();
    TypeName returnType = Utils.typeOf(method.getReturnType());

    ExecutableType resolvedMethodType = (ExecutableType) context.getApc().types().asMemberOf(sourceType, method);
    // 真实的方法参数类型
    List<? extends TypeMirror> actualParameterTypes = resolvedMethodType.getParameterTypes();
    List<String> parameterTypeNames = actualParameterTypes.stream().map(pt -> {
      TypeName type = Utils.typeOf(pt);
      return Utils.getSimpleName(type);
    }).toList();
    String parametersSignature = String.join(", ", parameterTypeNames);

    List<String> parameterNames = method.getParameters().stream()
      .map(param -> param.getSimpleName().toString()).toList();

    // 检查参数名是否为arg0、arg1等形式，如果是，给出警告
    boolean hasArgParameters = parameterNames.stream().anyMatch(name -> name.matches("arg\\d+"));
    if (hasArgParameters) {
      context.getApc().messaging()
        .warning("LabzenWebProcessor: The parameters name of Method [" + methodName + "] are defined of arg0 or arg1, " +
          "this can lead to incorrect parameter names in the generated code. " +
          "Please be sure the 'maven-compiler-plugin' plugin has config '<parameters>true</parameters>' in pom.xml file, " +
          "used to preserve method parameter names when enabling compilation.");
    }

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
//    elementMethod.getParameters().addAll(methodParameters);

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

  /**
   * 读取方法参数及其注解信息
   *
   * @param parameterElements 参数元素列表
   * @param actualParameterTypes 实际参数类型列表
   * @return 参数元素列表
   */
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

  /**
   * 从注解镜像列表中提取注解信息
   *
   * @param annotationMirrors 注解镜像列表
   * @return 注解元素列表
   */
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

  /**
   * 解析方法上的注解，生成代码建议
   *
   * @param method 方法元素
   */
  private void parseMethodAnnotations(ElementMethod method) {
    List<? extends Suggestion> suggestions = method.getAnnotations().stream()
      .flatMap(annotation ->
        evaluators.stream().flatMap(evaluator -> {
          TypeName type = annotation.getType();
          if (evaluator.support(type)) {
            return evaluator.evaluate(context.getApc().config(), type, annotation.getMembers()).stream();
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

  /**
   * 解析追加建议，添加字段或注解
   *
   * @param method 方法元素
   * @param suggestion 追加建议
   */
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

  /**
   * 解析移除建议，删除字段或注解
   *
   * @param method 方法元素
   * @param suggestion 移除建议
   */
  private void parseRemoveSuggestion(ElementMethod method, RemoveSuggestion suggestion) {
    if (ElementClass.class.equals(suggestion.kind())) {
      removeNeedlessElements(elementClass.getFields(), field -> field.keyword().equals(suggestion.keyword()));

      removeNeedlessElements(elementClass.getAnnotations(), annotation -> annotation.keyword().equals(suggestion.keyword()));
    } else if (ElementMethod.class.equals(suggestion.kind())) {
      removeNeedlessElements(method.getAnnotations(), annotation -> annotation.keyword().equals(suggestion.keyword()));
    }
  }

  /**
   * 解析替换建议，修改方法体或注解属性
   *
   * @param method 方法元素
   * @param suggestion 替换建议
   */
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

  /**
   * 处理废弃方法，将方法体置空并保留必要注解
   *
   * @param method 方法元素
   */
  private void parseDiscardSuggestion(ElementMethod method) {
    // 移除方法上的注解，忽略Override, Nonnull等
    removeNeedlessElements(method.getAnnotations(), annotation -> !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(((ElementAnnotation) annotation).getType()));

    method.getParameters().forEach(parameter -> {
      // 移除方法参数的注解，忽略Override, Nonnull等
      removeNeedlessElements(parameter.getAnnotations(), annotation -> !RESERVED_ANNOTATIONS_WHEN_DISCARD_METHOD.contains(((ElementAnnotation) annotation).getType()));
    });

    method.setBody(new ElementMethodBody("", "", java.util.Collections.emptyList()));
  }

  /**
   * 从集合中移除符合条件的元素
   *
   * @param elements 元素集合
   * @param condition 移除条件
   */
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
