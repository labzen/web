package cn.labzen.web.apt.evaluate.generics;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.apt.internal.Utils;
import cn.labzen.web.apt.internal.context.AnnotationProcessorContext;
import cn.labzen.web.apt.internal.element.ElementAnnotation;
import cn.labzen.web.apt.internal.element.ElementClass;
import cn.labzen.web.apt.internal.element.ElementField;
import cn.labzen.web.apt.suggestion.AppendSuggestion;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import static cn.labzen.web.apt.definition.TypeNames.ANNOTATION_JAKARTA_RESOURCE;

/**
 * 核心业务逻辑类泛型指向
 * <p>
 * 模板方法类，处理 StandardController、FileController 等继承体系中的服务层依赖注入。
 * 子类实现 supportedInterfaceName() 方法声明支持的接口类型，
 * 父类负责处理泛型参数并生成 @Resource 字段。
 */
public abstract non-sealed class PrimaryServiceGenericsAssign implements InterfaceGenericsEvaluator {

  protected TypeName resourceType;
  protected TypeName supportedInterfaceType;

  /**
   * 初始化评价器，加载必要类型信息
   *
   * @param context 注解处理器上下文
   */
  @Override
  public void init(AnnotationProcessorContext context) {
    resourceType = TypeName.get(context.elements().getTypeElement(ANNOTATION_JAKARTA_RESOURCE).asType());
    supportedInterfaceType = TypeName.get(context.elements().getTypeElement(supportedInterfaceName()).asType());
  }

  /**
   * 返回支持的父接口名称
   *
   * @return 接口完全限定名
   */
  protected abstract String supportedInterfaceName();

  /**
   * 判断是否支持该父接口类型
   * <p>
   * 支持参数化类型和原始类型两种判断方式。
   *
   * @param type 父接口类型
   * @return 是否支持
   */
  @Override
  public final boolean support(TypeName type) {
    if (type instanceof ParameterizedTypeName parameterizedTypeName) {
      if (supportedInterfaceType instanceof ParameterizedTypeName parameterizedInterfaceType) {
        return parameterizedInterfaceType.rawType.equals(parameterizedTypeName.rawType);
      }
      return supportedInterfaceType.equals(parameterizedTypeName.rawType);
    } else {
      return supportedInterfaceType.equals(type);
    }
  }

  /**
   * 根据泛型类型生成服务层依赖注入字段建议
   * <p>
   * 将泛型类型转换为驼峰命名的字段名，并添加 @Resource 注解。
   *
   * @param primaryComponentClass 服务层类类型
   * @return 添加字段的建议列表
   */
  protected List<AppendSuggestion> internalServiceBeanEvaluate(TypeName primaryComponentClass) {
    String simpleName = Utils.getSimpleName(primaryComponentClass);
    String fieldName = Strings.camelCase(simpleName);
    ElementAnnotation annotation = new ElementAnnotation(resourceType);
    ElementField elementField = new ElementField(fieldName, primaryComponentClass, List.of(annotation));

    return List.of(new AppendSuggestion(elementField, ElementClass.class));
  }
}
