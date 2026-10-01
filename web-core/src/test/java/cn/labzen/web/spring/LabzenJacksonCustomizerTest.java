package cn.labzen.web.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LabzenJacksonCustomizer} 的配置效果测试。
 * <p>
 * 验证定制器启用 {@code FAIL_ON_NULL_FOR_PRIMITIVES}，且未定制时保持 Jackson 默认行为。
 */
class LabzenJacksonCustomizerTest {

  @Test
  void shouldFailWhenPrimitiveReceivesExplicitNull() {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    new LabzenJacksonCustomizer().customize(builder);
    ObjectMapper mapper = builder.build();

    assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    assertThrows(MismatchedInputException.class, () -> mapper.readValue("{\"frozen\":null}", PrimitiveBean.class));
  }

  @Test
  void shouldNotFailByDefault() throws Exception {
    ObjectMapper mapper = new Jackson2ObjectMapperBuilder().build();

    assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    PrimitiveBean bean = mapper.readValue("{\"frozen\":null}", PrimitiveBean.class);
    assertFalse(bean.isFrozen());
  }

  public static class PrimitiveBean {

    private boolean frozen;

    public boolean isFrozen() {
      return frozen;
    }

    public void setFrozen(boolean frozen) {
      this.frozen = frozen;
    }
  }
}
