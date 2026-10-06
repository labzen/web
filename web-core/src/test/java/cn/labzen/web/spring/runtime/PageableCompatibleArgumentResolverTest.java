package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页参数解析器的接管判据测试。
 * <p>
 * 仅接管表单/query 来源的 {@link Pageable} 参数；{@code @RequestBody} 等其他来源不接管。
 */
class PageableCompatibleArgumentResolverTest {

  private final PageableCompatibleArgumentResolver resolver = new PageableCompatibleArgumentResolver();
  private final LabzenPageableRequestBodyAdvice advice = new LabzenPageableRequestBodyAdvice();

  @Test
  void shouldSupportFormBoundPageableParameter() throws Exception {
    assertTrue(resolver.supportsParameter(parameterOf("modelBound", SampleResource.class)));
    assertTrue(resolver.supportsParameter(parameterOf("plain", SampleResource.class)));
  }

  @Test
  void shouldNotSupportOtherBoundPageableParameter() throws Exception {
    assertFalse(resolver.supportsParameter(parameterOf("bodyBound", SampleResource.class)));
    assertFalse(resolver.supportsParameter(parameterOf("requestParamBound", SampleResource.class)));
    assertFalse(resolver.supportsParameter(parameterOf("pathVariableBound", SampleResource.class)));
  }

  @Test
  void shouldNotSupportNonPageableParameter() throws Exception {
    assertFalse(resolver.supportsParameter(parameterOf("bodyBoundPlain", PlainResource.class)));
  }

  @Test
  void adviceShouldSupportPageableBodyParameterOnly() throws Exception {
    assertTrue(advice.supports(parameterOf("bodyBound", SampleResource.class), Object.class, null));
    assertFalse(advice.supports(parameterOf("bodyBoundPlain", PlainResource.class), Object.class, null));
  }

  private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws Exception {
    Method method = SampleController.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  @SuppressWarnings("unused")
  private static class SampleController {

    void modelBound(@ModelAttribute SampleResource resource) {
    }

    void plain(SampleResource resource) {
    }

    void bodyBound(@RequestBody SampleResource resource) {
    }

    void requestParamBound(@RequestParam("resource") SampleResource resource) {
    }

    void pathVariableBound(@PathVariable("resource") SampleResource resource) {
    }

    void bodyBoundPlain(@RequestBody PlainResource resource) {
    }
  }

  public static class SampleResource implements Pageable {

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
