package cn.labzen.web.meta

import cn.labzen.meta.configuration.annotation.Configured
import cn.labzen.meta.configuration.annotation.Item

@Configured("web")
interface WebConfiguration {

  /**
   * Controller的包路径，可以不指定
   */
  @Item(path = "controller.path", required = false)
  fun controllerPackage(): String?

  /**
   * 通过Controller接口的定义，生成动态Controller类的类型后缀
   */
  @Item(path = "controller.suffix", required = false, defaultValue = "Impl")
  fun controllerClassSuffix(): String

  /**
   * 开启 API 版本控制
   */
  @Item(path = "controller.version.enable", required = false, defaultValue = "false")
  fun controllerVersionEnabled(): Boolean

  /**
   * API 版本控制的版本前缀，默认大写 V
   */
  @Item(path = "controller.version.prefix", required = false, defaultValue = "v")
  fun controllerVersionPrefix(): String

  /**
   * API 版本控制的基础版本，默认从 1 开始
   */
  @Item(path = "controller.version.base", required = false, defaultValue = "1")
  fun controllerVersionBase(): Int

  /**
   * API 版本控制，version 的位置，支持 HEAD（默认）, URI, PARAM
   *
   * HEAD - 通过请求头部信息 Accept: 来传递请求 API 的版本信息，例如：'Accept: application/vnd.app.v1+json'
   * URI - 通过 API 的请求地址前置版本信息，例如 'https://www.app.com/v1/login'
   * PARAM - 通过请求 API 时，使用参数来传递版本信息，例如 'https://www.app.com/login?version=v1'
   */
  @Item(path = "controller.version.place", required = false, defaultValue = "URI")
  fun controllerVersionPlace(): RequestMappingVersionPlace

  /**
   * API 版本控制的名称，当 controller.version.place 为 HEAD 时有效
   */
  @Item(path = "controller.version.head", required = false, defaultValue = "app")
  fun controllerVersionVNDName(): String

  /**
   * API 版本控制的名称，当 controller.version.place 为 PARAM 时有效
   */
  @Item(path = "controller.version.param", required = false, defaultValue = "version")
  fun controllerVersionParamName(): String

  /**
   * Controller的接口实现类是否保存为文件
   */
  @Item(path = "controller.file.save", required = false, defaultValue = "false")
  fun writeClassFile(): Boolean

  /**
   * Controller的接口实现类保存的目录
   */
  @Item(path = "controller.file.path", required = false, defaultValue = "")
  fun writeClassDirectory(): String

  /**
   * 定义统一的 API 路径前缀，默认：/api  如需自定义，则将该值设置为空字符
   */
  @Item(path = "api.path.prefix", required = false, defaultValue = "api")
  fun apiPathPrefix(): String

  /**
   * 启用统一 Response 响应内容格式
   */
  @Item(path = "response.rest.unify.enable", required = false, defaultValue = "true")
  fun unifyRestResponse(): Boolean

  /**
   * 如false，则没有通过Controller接口定义的response，不会进行格式统一
   */
  @Item(path = "response.rest.unify.all", required = false, defaultValue = "true")
  fun unifyAllRestResponse(): Boolean

  /**
   * 可提供自定义的响应内容转换器（实现 ResponseTransformer），将会先从 Spring 容器中寻找对应的 Bean，如果不存在则创建一个实例；如果均失败则会使用默认的转换器
   */
  @Item(path = "response.rest.unify.transformer", required = false, defaultValue = "")
  fun unifyRestResponseTransformer(): String

}