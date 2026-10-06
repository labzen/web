package cn.labzen.web.paging.internal;

import cn.labzen.web.api.paging.Pageable;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link PageableSupport} 的前置判断测试。
 * <p>
 * 实例为空、未实现 {@link Pageable}、或无法获取请求时，均原样返回实例（不解析分页条件）。
 */
class PageableSupportTest {

  private final MethodParameter pageableParameter = parameterOf("pageable", PageableResource.class);
  private final MethodParameter plainParameter = parameterOf("plain", PlainResource.class);

  @Test
  void shouldReturnNullWhenAttributeAbsent() {
    assertNull(PageableSupport.attachPaging(pageableParameter, null, null));
  }

  @Test
  void shouldReturnAttributeAsIsWhenNotPageable() {
    PlainResource attribute = new PlainResource();

    assertSame(attribute, PageableSupport.attachPaging(plainParameter, attribute, webRequest()));
  }

  @Test
  void shouldReturnAttributeAsIsWhenWebRequestAbsent() {
    PageableResource attribute = new PageableResource();

    assertSame(attribute, PageableSupport.attachPaging(pageableParameter, attribute, null));
  }

  private NativeWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest("GET", "/demo"));
  }

  private MethodParameter parameterOf(String methodName, Class<?> parameterType) {
    try {
      Method method = Holder.class.getDeclaredMethod(methodName, parameterType);
      return new MethodParameter(method, 0);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unused")
  private static class Holder {

    void pageable(PageableResource resource) {
    }

    void plain(PlainResource resource) {
    }
  }

  public static class PageableResource implements Pageable {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class PlainResource {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
