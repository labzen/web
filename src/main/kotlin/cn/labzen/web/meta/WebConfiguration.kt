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
   * Controller的接口实现类是否保存为文件
   */
  @Item(path = "controller.file.save", required = false, defaultValue = "true")
  fun writeClassFile(): Boolean

  /**
   * Controller的接口实现类保存的目录
   */
  @Item(path = "controller.file.path", required = false, defaultValue = "")
  fun writeClassDirectory(): String

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