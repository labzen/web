package cn.labzen.web.meta

import cn.labzen.meta.configuration.annotation.Configured
import cn.labzen.meta.configuration.annotation.Item
import cn.labzen.web.defination.APIVersionCarrier
import cn.labzen.web.paging.PageConverter
import cn.labzen.web.paging.Pageable
import cn.labzen.web.paging.Pagination
import cn.labzen.web.response.format.ResponseFormatter

@Configured("web")
interface WebConfiguration {

  /**
   * Controller的包路径，可以不指定
   */
//  @Item(path = "controller.path", required = false)
//  fun controllerPackage(): String?

  /**
   * 通过Controller接口的定义，生成动态Controller类的类型后缀
   */
//  @Item(path = "controller.suffix", required = false, defaultValue = "Impl")
//  fun controllerClassSuffix(): String

  /**
   * Controller的接口实现类是否保存为文件
   */
//  @Item(path = "controller.file.save", required = false, defaultValue = "false")
//  fun saveClassFile(): Boolean

  /**
   * Controller的接口实现类保存的目录
   */
//  @Item(path = "controller.file.path", required = false, defaultValue = "")
//  fun writeClassDirectoryTo(): String

  /**
   * 忽略Controller的接口实现类生成时的 WARN 日志
   */
//  @Item(path = "controller.log.no-warn", required = false, defaultValue = "false")
//  fun ignoreControllerSourceWarning(): Boolean

  /**
   * 开启 API 版本控制
   */
//  @Item(path = "controller.version.enable", required = false, defaultValue = "false")
//  fun controllerVersionEnabled(): Boolean

  /**
   * API 版本控制的版本前缀，默认小写 v
   */
//  @Item(path = "controller.version.prefix", required = false, defaultValue = "v")
//  fun controllerVersionPrefix(): String

  /**
   * API 版本控制的基础版本，默认从 1 开始
   */
//  @Item(path = "controller.version.base", required = false, defaultValue = "1")
//  fun controllerVersionBase(): Int

  /**
   * API 版本控制，version 的位置，支持 HEADER（默认）, URI, PARAM
   *
   * 默认使用 HEADER，这里需要***注意***，如果在 `labzen.web.config` 中修改了 `processor.api-version.carrier` 配置，`labzen.yml` 中这个配置也要相应修改为一致的值
   *
   * DISABLE - 禁用版本控制！
   * HEADER - 通过请求头部信息 Accept: 来传递请求 API 的版本信息，例如：'Accept: application/vnd.app.v1+json'
   * URI - 通过 API 的请求地址前置版本信息，例如 'https://www.app.com/v1/login'
   * PARAMETER - 通过请求 API 时，使用参数来传递版本信息，例如 'https://www.app.com/login?version=v1'
   */
  // todo 考虑个合适的方式，在生成代码的时候，通过某个合适途径，将这个值传递给runtime环境，而不是两边配置
  @Item(path = "api-version.carrier", required = false, defaultValue = "HEADER")
  fun apiVersionCarrier(): APIVersionCarrier

  /**
   * API 版本控制的名称，当 controller.version.place 为 HEADER 时有效
   */
//  @Item(path = "controller.version.header-vnd", required = false, defaultValue = "app")
//  fun controllerVersionVNDName(): String

  /**
   * API 版本控制强制要求访问API时带有Accept Header信息，默认false，如果访问API的Header种没有Accept，
   * Spring默认会选择一个可以匹配的 produces 方法进行响应，当 api-version.carrier 为 HEADER 时有效
   */
  @Deprecated("再考虑考虑，这个是不是真的有必要")
  @Item(path = "api-version.header-accept-forced", required = false, defaultValue = "true")
  fun apiVersionHeaderAcceptForced(): Boolean

  /**
   * API 版本控制的名称，当 controller.version.place 为 PARAM 时有效
   */
//  @Item(path = "controller.version.param", required = false, defaultValue = "version")
//  fun controllerVersionParamName(): String

  /**
   * 定义统一的 API 路径前缀，默认：/api；可根据项目实际情况自定义，如不需要前缀则将该值设置为空字符
   */
  @Item(path = "api-path-prefix", required = false, defaultValue = "api")
  fun apiPathPrefix(): String

  /**
   * 启用 Response 响应内容统一格式化，默认 true，否则使用 Spring 默认的格式化
   */
  @Item(path = "response.formatting.enable", required = false, defaultValue = "true")
  fun responseFormattingEnabled(): Boolean

  /**
   * 默认会格式化所有返回类型为 [Result] 的响应数据。如果设置为 true，则会对所有的 Controller 返回类型进行格式化
   */
  @Item(path = "response.formatting.all-forced", required = false, defaultValue = "true")
  fun responseFormattingForcedAll(): Boolean

  /**
   * 提供自定义的响应内容格式化组件（实现 [ResponseFormatter]）类的 FQCN。将会尝试先从 Spring 容器中寻找是否有注册过的组件对象，如果不存在则创建一个实例；如果均失败则会使用默认的格式化组件
   */
  // 改用SPI
//  @Item(path = "response.formatting.formatter", required = false, defaultValue = "")
//  fun responseFormatter(): String

  /**
   * 设置一个对 [Pageable] 和 [Pagination] 的转换器（实现 [PageConverter]）类的 FQCN。将会尝试先从 Spring 容器中寻找是否有注册过的组件对象，如果不存在则创建一个实例
   */
  @Item(path = "page-converter", required = false, defaultValue = "")
  fun pageConverter(): String

  /**
   * 设置一个 [Pagination] 的转换器（实现 [PaginationConverter]）类的 FQCN。将会尝试先从 Spring 容器中寻找是否有注册过的组件对象，如果不存在则创建一个实例
   */
//  @Item(path = "page.converter-page", required = false, defaultValue = "")
//  fun pageConverterForResponse(): String

  /**
   * 设置默认分页大小，默认20
   */
  @Item(path = "page-size", required = false, defaultValue = "20")
  fun pageSize(): Int
}