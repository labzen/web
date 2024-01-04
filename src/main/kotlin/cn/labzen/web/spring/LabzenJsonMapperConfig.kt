package cn.labzen.web.spring

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanPostProcessor

class LabzenJsonMapperConfig : BeanPostProcessor {

  override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
    if (bean is ObjectMapper) {
      // todo 不适用spring默认的全局 ObjectMapper，这样会影响上层项目的设置
      bean.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
    return super.postProcessAfterInitialization(bean, beanName)
  }
}