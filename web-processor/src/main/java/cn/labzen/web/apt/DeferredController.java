package cn.labzen.web.apt;

import javax.lang.model.element.TypeElement;

/**
 * 延迟处理的控制器，记录重试次数以避免无限延迟
 *
 * @param element  控制器类型元素
 * @param retries  已重试次数
 */
public record DeferredController(TypeElement element, int retries) {

  /**
   * 最大重试次数，超过此次数后不再重试，直接报告编译错误
   */
  public static final int MAX_RETRIES = 3;

  public DeferredController(TypeElement element) {
    this(element, 0);
  }

  /**
   * 返回重试次数 +1 的新实例
   */
  public DeferredController incrementRetry() {
    return new DeferredController(element, retries + 1);
  }

  /**
   * 是否还可以重试
   */
  public boolean canRetry() {
    return retries < MAX_RETRIES;
  }
}
