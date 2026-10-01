package cn.labzen.web.spring.runtime;

import cn.labzen.web.log.ApiLogMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link LabzenHandlerExceptionResolver} 的异常信封测试。
 * <p>
 * 覆盖请求体无法解析（JSON 语法错误 / 类型不匹配）、表单绑定失败、请求体校验失败三类异常，
 * 验证它们统一输出包含 400 状态码的标准响应信封。
 * <p>
 * 注意：该解析器将状态码写入响应信封的 {@code code} 字段，不改变 HTTP 响应状态。
 */
class LabzenHandlerExceptionResolverTest {

  private LabzenHandlerExceptionResolver resolver;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() throws Exception {
    resolver = new LabzenHandlerExceptionResolver();
    setField("converters", List.of(new MappingJackson2HttpMessageConverter()));
    setField("apiLogMessageBuilder", mock(ApiLogMessageBuilder.class));

    request = new MockHttpServletRequest("POST", "/api/demo");
    request.setRequestURI("/api/demo");
    response = new MockHttpServletResponse();
  }

  private void setField(String name, Object value) throws Exception {
    var field = LabzenHandlerExceptionResolver.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(resolver, value);
  }

  private String body() {
    return new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  void shouldReturnUnifiedEnvelopeWhenRequestBodyNotReadable() {
    var exception = new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);

    ModelAndView mav = resolver.resolveException(request, response, new Object(), exception);

    assertNotNull(mav);
    String body = body();
    assertTrue(body.contains("\"code\":400"), body);
    assertTrue(body.contains("JSON parse error"), body);
  }

  @Test
  void shouldReturnValidatorMapWhenFormBindingFails() {
    var bindingResult = new BeanPropertyBindingResult(new DemoBean(), "demoBean");
    bindingResult.addError(new FieldError("demoBean", "name", "must not be blank"));

    ModelAndView mav = resolver.resolveException(request, response, new Object(), new BindException(bindingResult));

    assertNotNull(mav);
    String body = body();
    assertTrue(body.contains("\"code\":400"), body);
    assertTrue(body.contains("\"validator\""), body);
    assertTrue(body.contains("\"name\""), body);
    assertTrue(body.contains("must not be blank"), body);
  }

  @Test
  void shouldReturnValidatorMapWhenRequestBodyValidationFails() throws Exception {
    Method method = DemoController.class.getDeclaredMethod("create", DemoBean.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    var bindingResult = new BeanPropertyBindingResult(new DemoBean(), "demoBean");
    bindingResult.addError(new FieldError("demoBean", "name", "must not be blank"));

    var exception = new MethodArgumentNotValidException(parameter, bindingResult);

    ModelAndView mav = resolver.resolveException(request, response, new Object(), exception);

    assertNotNull(mav);
    String body = body();
    assertTrue(body.contains("\"code\":400"), body);
    assertTrue(body.contains("\"validator\""), body);
    assertTrue(body.contains("\"name\""), body);
  }

  @Test
  void shouldFallThroughForUnhandledException() {
    ModelAndView mav = resolver.resolveException(request, response, new Object(),
        new IllegalStateException("boom"));

    assertNull(mav);
  }

  public static class DemoBean {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class DemoController {

    public void create(DemoBean resource) {
      // no-op
    }
  }
}
