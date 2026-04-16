package cn.labzen.web.spring;

import cn.labzen.meta.Labzens;
import cn.labzen.meta.spring.SpringInitializationOrder;
import cn.labzen.spring.Springs;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.paging.PageConverter;
import cn.labzen.web.meta.WebCoreConfiguration;
import cn.labzen.web.paging.convert.NonePageConverter;
import cn.labzen.web.paging.convert.PageConverterHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;

/**
 * Web 组件上下文初始化器
 * <p>
 * 在 Spring 容器刷新之前执行，初始化 Web 组件所需的必要数据。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>加载并注册 PageConverter 实例</li>
 * </ul>
 *
 * @see ApplicationContextInitializer
 */
@Slf4j
public class LabzenWebContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

  private static final String NONE_PAGE_CONVERTER_FQCN = NonePageConverter.class.getName();

  /**
   * 初始化 Web 组件必要数据
   * <p>
   * 1. 从配置中获取 PageConverter 的全限定类名
   * 2. 动态加载 PageConverter 类并创建实例
   * 3. 注册到 PageConverterHolder 供后续使用
   */
  @Override
  public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    String pageConverterFQCN = Strings.valueWhenBlank(configuration.pageConverter(), NONE_PAGE_CONVERTER_FQCN);

    try {
      Class<?> pageConverterClass = ClassUtils.forName(pageConverterFQCN, applicationContext.getClassLoader());
      if (PageConverter.class.isAssignableFrom(pageConverterClass)) {
        PageConverter<?> converter = ((PageConverter<?>) Springs.getOrCreate(pageConverterClass));
        PageConverterHolder.setConverter(converter);
      }
    } catch (ClassNotFoundException e) {
      logger.error("初始化 PageConverter 实例异常", e);
    }
  }

  @Override
  public int getOrder() {
    return SpringInitializationOrder.MODULE_WEB_INITIALIZER_ORDER;
  }
}
