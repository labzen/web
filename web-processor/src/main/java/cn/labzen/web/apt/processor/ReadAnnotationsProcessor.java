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
 * 读取 Controller 接口上的所有注解
 * <p>
 * 遍历接口上标注的所有注解，根据注解类型生成对应的代码生成建议：
 * <ul>
 *   <li>AppendSuggestion - 添加新的注解或字段</li>
 *   <li>RemoveSuggestion - 移除不需要的注解或字段</li>
 *   <li>ReplaceSuggestion - 替换或修改现有注解的属性</li>
 * </ul>
 */
public final class ReadAnnotationsProcessor implements InternalProcessor {

  /**
   * 处理接口上的所有注解
   *
   * @param context 控制器上下文
   */
  @Override
  public void process(ControllerContext context) {
    // 获取接口声明的所有注解，将直接复制给生成的Controller实现类
    context.getSource().getAnnotationMirrors().forEach(annotationMirror -> processPerAnnotation(context, annotationMirror));
  }

  /**
   * 处理单个注解
   * <p>
   * 核心逻辑：
   * <ul>
   *   <li>1. 将注解转换为 ElementAnnotation 对象并添加到根节点</li>
   *   <li>2. 遍历所有评价器，收集代码生成建议</li>
   *   <li>3. 根据建议类型执行相应的解析操作</li>
   * </ul>
   * @param context 控制器上下文
   * @param annotationMirror 注解镜像
   */
  private void processPerAnnotation(ControllerContext context, AnnotationMirror annotationMirror) {
    ClassName annotationClass = Utils.classOf(annotationMirror.getAnnotationType().asElement());
    Map<String, Object> annotationMembers = Utils.readAnnotationMembers(annotationMirror);

    ElementAnnotation annotation = new ElementAnnotation(annotationClass, annotationMembers);
    context.getRoot().getAnnotations().add(annotation);

    Stream<? extends Suggestion> suggestions = context.getAnnotationEvaluators().stream()
      .flatMap(evaluator -> {
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

  /**
   * 解析追加建议，将新元素添加到根节点
   *
   * @param root 根节点
   * @param suggestion 追加建议
   */
  private void parseAppendSuggestion(ElementClass root, AppendSuggestion suggestion) {
    if (suggestion.element() instanceof ElementField elementField) {
      root.getFields().add(elementField);
    } else if (suggestion.element() instanceof ElementAnnotation elementAnnotation && ElementClass.class.equals(suggestion.kind())) {
      root.getAnnotations().add(elementAnnotation);
    }
  }

  /**
   * 解析移除建议，从根节点删除指定元素
   *
   * @param root 根节点
   * @param suggestion 移除建议
   */
  private void parseRemoveSuggestion(ElementClass root, RemoveSuggestion suggestion) {
    if (!ElementClass.class.equals(suggestion.kind())) {
      return;
    }

    removeNeedlessElements(root.getFields(), element -> element.keyword().equals(suggestion.keyword()));
    removeNeedlessElements(root.getAnnotations(), element -> element.keyword().equals(suggestion.keyword()));
  }

  /**
   * 解析替换建议，修改现有注解的属性值
   *
   * @param root 根节点
   * @param suggestion 替换建议
   */
  private void parseReplaceSuggestion(ElementClass root, ReplaceSuggestion suggestion) {
    if (suggestion.element() instanceof ElementAnnotation elementAnnotation) {
      Optional<ElementAnnotation> found = root.getAnnotations().stream()
        .filter(annotation -> annotation.keyword().equals(suggestion.keyword())).findFirst();
      found.ifPresent(annotation -> annotation.getMembers().putAll(elementAnnotation.getMembers()));
    }
  }

  /**
   * 从集合中移除符合条件的元素
   * <p>
   * 先过滤再遍历删除，避免在迭代过程中直接修改集合导致的并发修改异常。
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
    return PRIORITY_READ_ANNOTATION;
  }
}
