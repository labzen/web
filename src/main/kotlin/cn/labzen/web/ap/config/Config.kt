package cn.labzen.web.ap.config

import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_BASED
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_CARRIER
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_HEADER_VND
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_PARAMETER_NAME
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_PREFIX
import cn.labzen.web.ap.config.ConfigKeys.CLASS_NAME_SUFFIX
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_BASED_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_CARRIER_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_HEADER_VND_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_PARAMETER_NAME_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_PREFIX_VALUE
import cn.labzen.web.ap.config.ConfigValues.CLASS_NAME_SUFFIX_VALUE
import cn.labzen.web.defination.APIVersionCarrier
import java.util.*

class Config internal constructor(private val properties: Properties) {

  fun classNameSuffix(): String {
    val original = properties.getOrDefault(CLASS_NAME_SUFFIX, CLASS_NAME_SUFFIX_VALUE).toString()
    return original.ifBlank { CLASS_NAME_SUFFIX_VALUE }
  }

  fun apiVersionCarrier(): APIVersionCarrier {
    val carrier = properties.getOrDefault(API_VERSION_CARRIER, API_VERSION_CARRIER_VALUE).toString()
    return try {
      APIVersionCarrier.valueOf(carrier)
    } catch (e: Exception) {
      println("未配置有效的 processor.api-version.carrier ，将使用 DISABLE，生成的 Controller 实现类将禁用API版本控制能力")
      APIVersionCarrier.DISABLE
    }
  }

  fun apiVersionPrefix(): String {
    val original = properties.getOrDefault(API_VERSION_PREFIX, API_VERSION_PREFIX_VALUE).toString()
    return original.ifBlank { API_VERSION_PREFIX_VALUE }
  }

  fun apiVersionBased(): Int {
    val based = properties.getOrDefault(API_VERSION_BASED, API_VERSION_BASED_VALUE).toString()
    return based.toIntOrNull() ?: let {
      println("未配置有效的 processor.api-version.based ，默认使用 1")
      1
    }
  }

  fun apiVersionHeaderVND(): String {
    val original = properties.getOrDefault(API_VERSION_HEADER_VND, API_VERSION_HEADER_VND_VALUE).toString()
    return original.ifBlank { API_VERSION_HEADER_VND_VALUE }
  }

  fun apiVersionParameterName(): String {
    val original = properties.getOrDefault(API_VERSION_PARAMETER_NAME, API_VERSION_PARAMETER_NAME_VALUE).toString()
    return original.ifBlank { API_VERSION_PARAMETER_NAME_VALUE }
  }
}