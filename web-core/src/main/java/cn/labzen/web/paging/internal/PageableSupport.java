package cn.labzen.web.paging.internal;

import cn.labzen.web.api.paging.Pageable;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 分页能力附加工具。
 * <p>
 * 为已绑定的资源实例附加分页能力：从请求中解析分页条件，返回一个把 {@link Pageable} 接口方法
 * 指向该分页条件的代理对象，其余方法仍委托给原实例。
 * <p>
 * 表单/query 绑定路径（{@code PageableCompatibleArgumentResolver}）与请求体绑定路径
 * （{@code LabzenPageableRequestBodyAdvice}）共用本方法，分页附加逻辑只有一份。
 */
public final class PageableSupport {

  private PageableSupport() {
  }

  /**
   * 为已绑定的资源实例附加分页能力。
   * <p>
   * 实例为空、未实现 {@link Pageable}、或无法从请求中解析分页条件时，原样返回实例。
   *
   * @param parameter  方法参数
   * @param attribute  已绑定的资源实例
   * @param webRequest Web 请求，用于解析分页条件
   * @return 附加分页能力后的实例
   */
  @SuppressWarnings("ConditionCoveredByFurtherCondition")
  public static Object attachPaging(MethodParameter parameter, Object attribute, NativeWebRequest webRequest) {
    if (attribute == null || !(attribute instanceof Pageable) || webRequest == null) {
      return attribute;
    }
    Paging paging = PageableResolver.resolve(webRequest);
    return PageableDelegator.delegate(parameter, attribute, paging);
  }
}
