package cn.labzen.web.paging.internal;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebCoreConfigurationStub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 分页代理的状态隔离测试。
 * <p>
 * 代理类按资源类型缓存，但分页条件与原始实例存放在代理实例上 ——
 * 同一类型的多次调用必须各自使用本次调用传入的分页条件与原始实例。
 */
class PageableDelegatorTest {

  @BeforeAll
  static void bootstrapConfiguration() throws Exception {
    WebCoreConfigurationStub.install();
  }

  @Test
  void shouldKeepPagingPerProxyInstance() throws Exception {
    MethodParameter parameter = parameterOf("demo", DemoResource.class);

    DemoResource first = (DemoResource) PageableDelegator.delegate(parameter,
        new DemoResource(),
        new Paging(false, 3, 50, Collections.emptyList()));
    DemoResource second = (DemoResource) PageableDelegator.delegate(parameter,
        new DemoResource(),
        Paging.UNPAGED_PAGING);
    DemoResource third = (DemoResource) PageableDelegator.delegate(parameter,
        new DemoResource(),
        new Paging(false, 7, 30, Collections.emptyList()));

    assertEquals(3, first.pageNumber());
    assertEquals(50, first.pageSize());
    assertFalse(first.unpaged());

    assertTrue(second.unpaged());

    assertEquals(7, third.pageNumber());
    assertEquals(30, third.pageSize());
  }

  @Test
  void shouldDelegateOtherMethodsToOwnTarget() throws Exception {
    MethodParameter parameter = parameterOf("demo", DemoResource.class);
    DemoResource first = new DemoResource();
    first.setName("first");
    DemoResource second = new DemoResource();
    second.setName("second");

    DemoResource firstProxy = (DemoResource) PageableDelegator.delegate(parameter, first, Paging.DEFAULT_PAGING);
    DemoResource secondProxy = (DemoResource) PageableDelegator.delegate(parameter, second, Paging.DEFAULT_PAGING);

    assertEquals("first", firstProxy.getName());
    assertEquals("second", secondProxy.getName());
  }

  @Test
  void shouldOverrideConvertToMethod() throws Exception {
    MethodParameter parameter = parameterOf("convert", ConvertResource.class);
    ConvertResource proxy = (ConvertResource) PageableDelegator.delegate(parameter,
        new ConvertResource(),
        Paging.DEFAULT_PAGING);

    assertDoesNotThrow(() -> proxy.getClass().getDeclaredMethod("convertTo"),
        "代理类应覆写 convertTo，否则调用会落到接口默认实现");

    try {
      proxy.convertTo();
    } catch (UnsupportedOperationException e) {
      fail("convertTo 落到了接口默认实现，代理未覆写该 default 方法");
    }
  }

  private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws Exception {
    Method method = Holder.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class Holder {

    void demo(DemoResource resource) {
    }

    void convert(ConvertResource resource) {
    }
  }

  public static class DemoResource implements Pageable {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class ConvertResource implements Pageable {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
