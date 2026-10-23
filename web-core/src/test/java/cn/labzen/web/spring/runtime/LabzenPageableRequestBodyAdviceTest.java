package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebCoreConfigurationStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 请求体分页兼容处理的行为测试。
 * <p>
 * 覆盖：空体时创建资源实例并附加分页能力、分页条件来自 query string 或 JSON 请求体、
 * query string 优先于请求体、无法实例化的类型保持 {@code null}、非空体保持原实例内容。
 * <p>
 * 消息转换阶段传给 advice 的 {@code HttpInputMessage} 会被框架包装，测试统一用
 * {@link #wrap(HttpInputMessage)} 模拟这层包装，确保实现不依赖具体消息类型。
 */
class LabzenPageableRequestBodyAdviceTest {

  private final LabzenPageableRequestBodyAdvice advice = new LabzenPageableRequestBodyAdvice();

  @BeforeAll
  static void bootstrapConfiguration() throws Exception {
    WebCoreConfigurationStub.install();
  }

  @AfterEach
  void resetRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void shouldCreatePagedResourceForEmptyBody() throws Exception {
    MockHttpServletRequest request = bind(new MockHttpServletRequest("POST", "/demo"));

    Object resource = advice.handleEmptyBody(null,
        wrap(inputMessage(request)),
        parameterOf("bodyBoundDefault", DefaultPagingResource.class),
        Object.class,
        null);

    assertInstanceOf(DefaultPagingResource.class, resource);
    assertNotSame(DefaultPagingResource.class, resource.getClass());
    assertFalse(((DefaultPagingResource) resource).unpaged());
    assertEquals(Pageable.DEFAULT_PAGE_SIZE, ((DefaultPagingResource) resource).pageSize());
    assertEquals(1, ((DefaultPagingResource) resource).pageNumber());
  }

  @Test
  void shouldReadPagingFromQueryForEmptyBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo");
    request.setParameter("unpaged", "true");
    bind(request);

    Object resource = advice.handleEmptyBody(null,
        wrap(inputMessage(request)),
        parameterOf("bodyBoundUnpaged", UnpagedQueryResource.class),
        Object.class,
        null);

    assertTrue(((UnpagedQueryResource) resource).unpaged());
  }

  @Test
  void shouldReadPagingFromJsonBody() throws Exception {
    byte[] body = "{\"title\":\"\",\"unpaged\":true}".getBytes(StandardCharsets.UTF_8);
    MethodParameter parameter = parameterOf("bodyBoundBodyPaging", BodyPagingResource.class);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo");
    request.setContent(body);
    bind(request);

    HttpInputMessage message = advice.beforeBodyRead(wrap(inputMessage(request)),
        parameter,
        Object.class,
        null);
    assertEquals("{\"title\":\"\",\"unpaged\":true}",
        new String(StreamUtils.copyToByteArray(message.getBody()), StandardCharsets.UTF_8));

    Object resource = advice.afterBodyRead(new BodyPagingResource(), wrap(message), parameter, Object.class, null);

    assertNotSame(BodyPagingResource.class, resource.getClass());
    assertTrue(((BodyPagingResource) resource).unpaged());
  }

  @Test
  void shouldPreferQueryPagingOverJsonBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo");
    request.setParameter("pn", "2");
    request.setContent("{\"pageNumber\":5,\"pageSize\":50}".getBytes(StandardCharsets.UTF_8));
    bind(request);
    MethodParameter parameter = parameterOf("bodyBoundQueryPaging", BodyQueryPagingResource.class);

    HttpInputMessage message = advice.beforeBodyRead(wrap(inputMessage(request)), parameter, Object.class, null);
    Object resource = advice.afterBodyRead(new BodyQueryPagingResource(),
        wrap(message),
        parameter,
        Object.class,
        null);

    assertEquals(2, ((BodyQueryPagingResource) resource).pageNumber());
    assertEquals(Pageable.DEFAULT_PAGE_SIZE, ((BodyQueryPagingResource) resource).pageSize());
  }

  @Test
  void shouldReturnNullForPageableInterfaceType() throws Exception {
    MockHttpServletRequest request = bind(new MockHttpServletRequest("POST", "/demo"));

    assertNull(advice.handleEmptyBody(null,
        wrap(inputMessage(request)),
        parameterOf("bodyBoundInterface", Pageable.class),
        Object.class,
        null));
  }

  @Test
  void shouldReturnNullForNonPageableType() throws Exception {
    MockHttpServletRequest request = bind(new MockHttpServletRequest("POST", "/demo"));

    assertNull(advice.handleEmptyBody(null,
        wrap(inputMessage(request)),
        parameterOf("bodyBoundPlain", PlainResource.class),
        Object.class,
        null));
  }

  @Test
  void shouldKeepResourceContentForNonEmptyBody() throws Exception {
    MockHttpServletRequest request = bind(new MockHttpServletRequest("POST", "/demo"));
    ContentResource body = new ContentResource();
    body.setName("demo");

    Object resource = advice.afterBodyRead(body,
        wrap(inputMessage(request)),
        parameterOf("bodyBoundContent", ContentResource.class),
        Object.class,
        null);

    assertEquals("demo", ((ContentResource) resource).getName());
  }

  private MockHttpServletRequest bind(MockHttpServletRequest request) {
    RequestContextHolder.setRequestAttributes(new ServletWebRequest(request));
    return request;
  }

  private ServletServerHttpRequest inputMessage(MockHttpServletRequest request) {
    return new ServletServerHttpRequest(request);
  }

  /**
   * 模拟框架对 HttpInputMessage 的包装
   */
  private HttpInputMessage wrap(HttpInputMessage message) {
    return new HttpInputMessage() {

      @Override
      public HttpHeaders getHeaders() {
        return message.getHeaders();
      }

      @Override
      public InputStream getBody() {
        try {
          return new ByteArrayInputStream(StreamUtils.copyToByteArray(message.getBody()));
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    };
  }

  private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws Exception {
    Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class SampleController {

    void bodyBoundDefault(@RequestBody DefaultPagingResource resource) {
    }

    void bodyBoundUnpaged(@RequestBody UnpagedQueryResource resource) {
    }

    void bodyBoundBodyPaging(@RequestBody BodyPagingResource resource) {
    }

    void bodyBoundQueryPaging(@RequestBody BodyQueryPagingResource resource) {
    }

    void bodyBoundContent(@RequestBody ContentResource resource) {
    }

    void bodyBoundInterface(@RequestBody Pageable resource) {
    }

    void bodyBoundPlain(@RequestBody PlainResource resource) {
    }
  }

  public static class DefaultPagingResource implements Pageable {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class UnpagedQueryResource implements Pageable {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class BodyPagingResource implements Pageable {

    private String title;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }

  public static class BodyQueryPagingResource implements Pageable {

    private String title;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }

  public static class ContentResource implements Pageable {

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
