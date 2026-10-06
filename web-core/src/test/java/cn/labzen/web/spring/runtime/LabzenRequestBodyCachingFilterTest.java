package cn.labzen.web.spring.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 请求体可重复读的验证。
 * <p>
 * 覆盖：拦截器（模拟 API 日志）读取请求体后，{@code @RequestBody} 仍能正常绑定；
 * 以及包装器自身可多次读取、multipart 请求直接透传。
 */
class LabzenRequestBodyCachingFilterTest {

  @Test
  void shouldAllowBodyToBeReadByInterceptorAndController() throws Exception {
    List<String> captured = new ArrayList<>();
    HandlerInterceptor bodyReader = new HandlerInterceptor() {
      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {
        try (BufferedReader reader = request.getReader()) {
          captured.add(reader.lines().collect(Collectors.joining()));
        }
        return true;
      }
    };

    List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter(),
        new MappingJackson2HttpMessageConverter());

    MockMvc mvc = MockMvcBuilders.standaloneSetup(new EchoController())
                                 .setMessageConverters(converters.toArray(new HttpMessageConverter[0]))
                                 .addFilters(new LabzenRequestBodyCachingFilter())
                                 .addInterceptors(bodyReader)
                                 .build();

    mvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"labzen\"}"))
       .andExpect(status().isOk())
       .andExpect(content().string("labzen"));

    assertEquals(List.of("{\"name\":\"labzen\"}"), captured);
  }

  @Test
  void shouldServeBodyMultipleTimes() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/echo");
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    request.setContent("{\"name\":\"中\"}".getBytes(StandardCharsets.UTF_8));

    RepeatableReadRequestWrapper wrapper = new RepeatableReadRequestWrapper(request);

    try (BufferedReader first = wrapper.getReader()) {
      assertEquals("{\"name\":\"中\"}", first.lines().collect(Collectors.joining()));
    }
    assertEquals("{\"name\":\"中\"}",
        new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  void shouldDelegateMultipartReadsWithoutCaching() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload");
    request.setContentType("multipart/form-data; boundary=X");
    request.setContent("part-data".getBytes(StandardCharsets.UTF_8));

    RepeatableReadRequestWrapper wrapper = new RepeatableReadRequestWrapper(request);

    try (BufferedReader reader = wrapper.getReader()) {
      assertEquals("part-data", reader.lines().collect(Collectors.joining()));
    }
  }

  @RestController
  public static class EchoController {

    @PostMapping("/echo")
    public String echo(@RequestBody EchoDto dto) {
      return dto.getName();
    }
  }

  public static class EchoDto {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
