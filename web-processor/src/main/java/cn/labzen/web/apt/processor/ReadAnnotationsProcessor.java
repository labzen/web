package cn.labzen.web.apt.processor;

import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.ControllerContext;
import cn.labzen.web.apt.internal.element.Element;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementClass;
import cn.labzen.web.apt.internal.element.ElementField;
import cn.labzen.web.apt.suggestion.AppendSuggestion;
import cn.labzen.web.apt.suggestion.RemoveSuggestion;
import cn.labzen.web.apt.suggestion.ReplaceSuggestion;
import cn.labzen.web.apt.suggestion.Suggestion;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.AnnotationMirror;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 读取Controller接口的所有注解
 */
public final class ReadAnnotationsProcessor implements InternalProcessor {

  @Override
  public void process(ControllerContext context) {
    // 获取接口声明的所有注解，将直接复制给生成的Controller实现类
    context.getSource().getAnnotationMirrors().forEach(annotationMirror -> processPerAnnotation(context, annotationMirror));
  }

  private void processPerAnnotation(ControllerContext context, AnnotationMirror annotationMirror) {
    ClassName annotationClass = Utils.classOf(annotationMirror.getAnnotationType().asElement());
    Map<String, Object> annotationMembers = Utils.readAnnotationMembers(annotationMirror);

    ElementAnnotation annotation = new ElementAnnotation(annotationClass, annotationMembers);
    context.getRoot().getAnnotations().add(annotation);

    Stream<? extends Suggestion> suggestions = context.getAnnotationEvaluators().stream()
      .flatMap(evaluator -> {
        evaluator.init(context.getApc());
        if (evaluator.support(annotation.getType())) {
          return evaluator.evaluate(context.getApc().config(), annotation.getType(), annotation.getMembers()).stream();
        } else {
          return Stream.of();
        }
      });

    suggestions.forEach(suggestion -> {
      switch (suggestion) {
        case AppendSuggestion append -> parseAppendSuggestion(context.getRoot(), append);
        case ReplaceSuggestion replace -> parseReplaceSuggestion(context.getRoot(), replace);
        case RemoveSuggestion remove -> parseRemoveSuggestion(context.getRoot(), remove);
        default -> throw new IllegalStateException("Unexpected suggestion: " + suggestion);
      }
    });
  }

  private void parseAppendSuggestion(ElementClass root, AppendSuggestion suggestion) {
    if (suggestion.element() instanceof ElementField elementField) {
      root.getFields().add(elementField);
    } else if (suggestion.element() instanceof ElementAnnotation elementAnnotation && ElementClass.class.equals(suggestion.kind())) {
      root.getAnnotations().add(elementAnnotation);
    }
  }

  private void parseRemoveSuggestion(ElementClass root, RemoveSuggestion suggestion) {
    if (!ElementClass.class.equals(suggestion.kind())) {
      return;
    }

    removeNeedlessElements(root.getFields(), element -> element.keyword().equals(suggestion.keyword()));
    removeNeedlessElements(root.getAnnotations(), element -> element.keyword().equals(suggestion.keyword()));
  }

  private void parseReplaceSuggestion(ElementClass root, ReplaceSuggestion suggestion) {
    if (suggestion.element() instanceof ElementAnnotation elementAnnotation) {
      Optional<ElementAnnotation> found = root.getAnnotations().stream()
        .filter(annotation -> annotation.keyword().equals(suggestion.keyword())).findFirst();
      found.ifPresent(annotation -> annotation.getMembers().putAll(elementAnnotation.getMembers()));
    }
  }

  private void removeNeedlessElements(LinkedHashSet<? extends Element> elements, Function<Element, Boolean> condition) {
    List<? extends Element> needlessElements = elements.stream().filter(condition::apply).toList();
    for (Element element : needlessElements) {
      elements.remove(element);
    }
  }

  @Override
  public int priority() {
    return PRIORITY_READ_ANNOTATION;
  }
}
