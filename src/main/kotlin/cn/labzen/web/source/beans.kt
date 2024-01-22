package cn.labzen.web.source

import cn.labzen.web.annotation.MappingServiceVersion
import cn.labzen.web.meta.WebConfiguration
import com.google.common.collect.HashBiMap
import javassist.CtClass
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool

internal data class ControllerMeta(
  val configuration: WebConfiguration,
  val interfaceType: Class<*>,
  val className: String,
  val clazz: CtClass,
  val classFile: ClassFile,
  val constPool: ConstPool
) {

  lateinit var mainServiceFieldName: String
  val services = HashBiMap.create<String, Class<*>>()
  val apiVersion: Int? = if (configuration.controllerVersionEnabled()) {
    if (interfaceType.isAnnotationPresent(MappingServiceVersion::class.java)) {
      interfaceType.getAnnotation(MappingServiceVersion::class.java).value
    } else // 读取默认 API 版本
      configuration.controllerVersionBase()
  } else null

  val methodGenerator = ControllerMethodGenerator(this)
}
