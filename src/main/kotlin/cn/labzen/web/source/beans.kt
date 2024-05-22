package cn.labzen.web.source

import cn.labzen.web.annotation.MappingVersion
import cn.labzen.web.meta.WebConfiguration
import com.google.common.collect.HashBiMap
import javassist.CtClass
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool

/**
 * Controller接口元信息
 * @property configuration Labzen配置信息
 * @property interfaceType Controller 接口Class
 * @property className Controller 实现类名
 * @property clazz javassist Controller 实现类
 * @property classFile javassist 类文件
 * @property constPool javassist 常量池
 */
internal data class ControllerMeta(
  val configuration: WebConfiguration,
  val interfaceType: Class<*>,
  val className: String,
  val clazz: CtClass,
  val classFile: ClassFile,
  val constPool: ConstPool
) {

  /**
   * 在Controller中调用的主Service属性名
   */
  lateinit var mainServiceFieldName: String

  /**
   * 在Controller中会用到的Service集合
   */
  val services: HashBiMap<String, Class<*>> = HashBiMap.create<String, Class<*>>()

  /**
   * Controller的API基础版本，Controller中的所有RequestMapping方法的版本将会基于这个版本。只有当[configuration]中的API版本功能开启后才有效
   */
  val apiVersion: Int? = if (configuration.controllerVersionEnabled()) {
    if (interfaceType.isAnnotationPresent(MappingVersion::class.java)) {
      interfaceType.getAnnotation(MappingVersion::class.java).value
    } else {
      // 读取默认 API 版本
      configuration.controllerVersionBase()
    }
  } else null

  /**
   * 方法生成器
   */
  val methodGenerator = ControllerMethodGenerator(this)
}
