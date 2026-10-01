package cn.labzen.web.spring.runtime;

import cn.labzen.web.exception.RequestException;
import cn.labzen.web.log.ApiLogMessageBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link LabzenExceptionCatchingFilter} 的异常响应测试。
 * <p>
 * 验证逃逸到过滤器的异常同样输出标准响应信封，HTTP 响应状态码保持默认值，真实状态码承载于信封的 {@code code} 字段。
 */
class LabzenExceptionCatchingFilterTest {

  private LabzenExceptionCatchingFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() throws Exception {
    filter = new LabzenExceptionCatchingFilter();
    setField("objectMapper", new ObjectMapper());
    setField("apiLogMessageBuilder", mock(ApiLogMessageBuilder.class));

    request = new MockHttpServletRequest("GET", "/api/demo");
    response = new MockHttpServletResponse();
  }

  private void setField(String name, Object value) throws Exception {
    var field = LabzenExceptionCatchingFilter.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(filter, value);
  }

  private String body() {
    return new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  void shouldKeepHttpStatusForUnexpectedException() {
    FilterChain chain = (req, res) -> {
      throw new IllegalStateException("boom");
    };

    filter.doFilterInternal(request, response, chain);

    assertEquals(200, response.getStatus());
    assertTrue(body().contains("\"code\":500"), body());
  }

  @Test
  void shouldKeepHttpStatusForRequestException() {
    FilterChain chain = (req, res) -> {
      throw new RequestException(400, "bad request");
    };

    filter.doFilterInternal(request, response, chain);

    assertEquals(200, response.getStatus());
    assertTrue(body().contains("\"code\":400"), body());
  }
}
