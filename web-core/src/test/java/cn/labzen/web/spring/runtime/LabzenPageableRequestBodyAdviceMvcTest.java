package cn.labzen.web.spring.runtime;

import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebCoreConfigurationStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 请求体分页的 MVC 端到端测试。
 * <p>
 * 用完整的 Web 容器走 {@code RequestResponseBodyMethodProcessor} 链路，
 * 验证 {@code @RequestBody} 资源在控制器方法内确实是带分页能力的代理对象。
 */
class LabzenPageableRequestBodyAdviceMvcTest {

  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeAll
  static void bootstrapConfiguration() throws Exception {
    WebCoreConfigurationStub.install();
  }

  @BeforeEach
  void setUp() {
    ExportController.captured = null;
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(TestWebConfig.class);
    context.refresh();

    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @AfterEach
  void tearDown() {
    context.close();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void shouldAttachPagingProxyForJsonBody() throws Exception {
    mockMvc.perform(post("/demo/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"demo\",\"unpaged\":true}"))
           .andExpect(status().isOk());

    Object resource = ExportController.captured;
    assertNotNull(resource);
    assertNotSame(ExportResource.class, resource.getClass());
    assertEquals("demo", ((ExportResource) resource).getTitle());
    assertTrue(((ExportResource) resource).unpaged());
    assertDoesNotThrow(() -> ((ExportResource) resource).convertTo());
  }

  @Test
  void shouldUsePageNumberAndPageSizeFromJsonBody() throws Exception {
    mockMvc.perform(post("/demo/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"demo\",\"pageNumber\":4,\"pageSize\":10}"))
           .andExpect(status().isOk());

    ExportResource resource = (ExportResource) ExportController.captured;
    assertNotNull(resource);
    assertFalse(resource.unpaged());
    assertEquals(4, resource.pageNumber());
    assertEquals(10, resource.pageSize());
  }

  @Test
  void shouldAttachPagingProxyForEmptyBody() throws Exception {
    mockMvc.perform(post("/demo/export").contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk());

    Object resource = ExportController.captured;
    assertNotNull(resource);
    assertNotSame(ExportResource.class, resource.getClass());
    assertDoesNotThrow(() -> ((ExportResource) resource).convertTo());
  }

  @Configuration
  @EnableWebMvc
  static class TestWebConfig {

    @Bean
    ExportController exportController() {
      return new ExportController();
    }

    @Bean
    LabzenPageableRequestBodyAdvice pageableRequestBodyAdvice() {
      return new LabzenPageableRequestBodyAdvice();
    }
  }

  @RestController
  static class ExportController {

    static Object captured;

    @PostMapping("/demo/export")
    public String export(@RequestBody(required = false) ExportResource resource) {
      captured = resource;
      return "ok";
    }
  }

  public static class ExportResource implements Pageable {

    private String title;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }
}
