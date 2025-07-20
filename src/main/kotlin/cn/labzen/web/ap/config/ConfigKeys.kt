package cn.labzen.web.ap.config

import cn.labzen.web.annotation.LabzenController
import cn.labzen.web.defination.APIVersionCarrier

object ConfigKeys {

  /**
   * 注解 @[LabzenController] 的 Controller 接口，生成的实现类名后缀，默认 Impl
   */
  const val CLASS_NAME_SUFFIX = "processor.class-name-suffix"

  /**
   * API 版本控制，version 的位置，可选 DISABLE, HEADER（默认）, URI, PARAM
   *
   * 参考 [APIVersionCarrier] 枚举类
   *
   * DISABLE - 禁用版本控制！
   * HEADER - 通过请求头部信息 Accept: 来传递请求 API 的版本信息，例如：'Accept: application/vnd.app.v1+json'
   * URI - 通过 API 的请求地址前置版本信息，例如 'https://www.app.com/v1/login'
   * PARAMETER - 通过请求 API 时，使用参数来传递版本信息，例如 'https://www.app.com/login?version=v1'
   */
  const val API_VERSION_CARRIER = "processor.api-version.carrier"

  /**
   * API 版本控制的版本前缀，默认小写 v
   */
  const val API_VERSION_PREFIX = "processor.api-version.prefix"

  /**
   * API 版本控制的基础版本，默认从 1 开始
   */
  const val API_VERSION_BASED = "processor.api-version.based"

  /**
   * API 版本控制的名称，当 processor.api-version.carrier 为 HEADER 时有效，默认 app
   */
  const val API_VERSION_HEADER_VND = "processor.api-version.header-vnd"

  /**
   * API 版本控制的名称，当 processor.api-version.carrier 为 PARAM 时有效，默认 version
   */
  const val API_VERSION_PARAMETER_NAME = "processor.api-version.parameter-name"

}