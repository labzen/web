package cn.labzen.web.paging.internal;

import cn.labzen.web.api.paging.Order;
import cn.labzen.web.meta.WebCoreConfigurationStub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页条件解析测试。
 * <p>
 * 覆盖 JSON 请求体解析、query string 优先、请求属性回退，以及无任何分页条件时的默认值。
 */
class PageableResolverTest {

  @BeforeAll
  static void bootstrapConfiguration() throws Exception {
    WebCoreConfigurationStub.install();
  }

  @Test
  void shouldResolveUnpagedFromJsonBody() {
    Paging paging = PageableResolver.resolveFromBody(
        "{\"title\":\"\",\"unpaged\":true}".getBytes(StandardCharsets.UTF_8));

    assertNotNull(paging);
    assertTrue(paging.unpaged());
  }

  @Test
  void shouldResolvePagingAndOrdersFromJsonBody() {
    Paging paging = PageableResolver.resolveFromBody(
        "{\"pageNumber\":4,\"pageSize\":10,\"orders\":[\"name!\",\"age+\"]}".getBytes(StandardCharsets.UTF_8));

    assertNotNull(paging);
    assertEquals(4, paging.pageNumber());
    assertEquals(10, paging.pageSize());
    assertEquals(2, paging.orders().size());

    Order first = paging.orders().get(0);
    assertEquals("name", first.name());
    assertFalse(first.asc());

    Order second = paging.orders().get(1);
    assertEquals("age", second.name());
    assertTrue(second.asc());
    assertEquals("first", second.nulls());
  }

  @Test
  void shouldIgnoreBodyWithoutPagingFields() {
    assertNull(PageableResolver.resolveFromBody("{\"title\":\"demo\"}".getBytes(StandardCharsets.UTF_8)));
    assertNull(PageableResolver.resolveFromBody("not-json".getBytes(StandardCharsets.UTF_8)));
    assertNull(PageableResolver.resolveFromBody(new byte[0]));
  }

  @Test
  void shouldPreferQueryOverBodyAttribute() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo");
    request.setParameter("ps", "5");
    request.setAttribute(PageableResolver.PAGING_ATTRIBUTE,
        new Paging(false, 9, 90, Collections.emptyList()));

    Paging paging = PageableResolver.resolve(new ServletWebRequest(request));

    assertEquals(1, paging.pageNumber());
    assertEquals(5, paging.pageSize());
  }

  @Test
  void shouldFallbackToBodyAttributeWhenQueryHasNoPaging() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo");
    request.setAttribute(PageableResolver.PAGING_ATTRIBUTE, Paging.UNPAGED_PAGING);

    Paging paging = PageableResolver.resolve(new ServletWebRequest(request));

    assertTrue(paging.unpaged());
  }

  @Test
  void shouldUseDefaultPagingWhenNothingProvided() {
    Paging paging = PageableResolver.resolve(new ServletWebRequest(new MockHttpServletRequest("POST", "/demo")));

    assertFalse(paging.unpaged());
    assertEquals(1, paging.pageNumber());
    assertEquals(20, paging.pageSize());
  }
}
