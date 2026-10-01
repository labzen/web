package cn.labzen.web.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置定制器。
 * <p>
 * 以叠加方式（{@link Jackson2ObjectMapperBuilderCustomizer}）向应用侧的 ObjectMapper 追加配置，
 * 不替换 Spring Boot 的 ObjectMapper 构建链，也不覆盖应用自身的 Jackson 定制。
 * <p>
 * 当前追加的配置项：反序列化时，基本类型字段收到显式 {@code null} 抛出异常
 * （{@link DeserializationFeature#FAIL_ON_NULL_FOR_PRIMITIVES}）。
 */
public class LabzenJacksonCustomizer implements Jackson2ObjectMapperBuilderCustomizer {

  @Override
  public void customize(Jackson2ObjectMapperBuilder builder) {
    builder.featuresToEnable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
  }
}
