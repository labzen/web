package cn.labzen.web.source

import cn.labzen.cells.core.utils.Strings
import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.annotation.LabzenWeb
import cn.labzen.web.annotation.ServiceHandler
import cn.labzen.web.meta.WebConfiguration
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.bytecode.AnnotationsAttribute
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import javassist.bytecode.annotation.Annotation as CTAnnotation
import kotlin.Annotation as VMAnnotation

/**
 * Controller 类生成器
 */
internal class ControllerGenerator(private val configuration: WebConfiguration, private val interfaceType: Class<*>) {

  private lateinit var controllerMeta: ControllerMeta

  fun generate(): Class<*> {
    createDynamicClass()

    if (configuration.saveClassFile()) {
      val dir = if (Strings.isBlank(configuration.writeClassDirectoryTo())) {
        defaultClassFileDirectory
      } else {
        configuration.writeClassDirectoryTo()
      }
      controllerMeta.clazz.writeFile(dir)
      logger.info().status(Status.NOTE).scene("Controller")
        .log("Save dynamic mvc controller class [${controllerMeta.className}] to directory [$dir]")
    }

    return controllerMeta.clazz.toClass()
  }

  // --------  Class  ----------

  private fun copyClassAnnotations() {
    with(controllerMeta) {
      val interfaceAnnotations: Array<VMAnnotation> = interfaceType.annotations.filter {
        // 剔除掉 Labzen 的注解
        it.annotationClass.findAnnotations(LabzenWeb::class).isEmpty()
      }.toTypedArray()
      // 转换 Controller interface 的注解
      val classAnnotationAttribute =
        ControllerSourceGeneratorHelper.duplicateAnnotations(interfaceAnnotations, constPool)
      classFile.addAttribute(classAnnotationAttribute)
    }
  }

  private fun setClassFields() {
    with(controllerMeta) {
      // 日志字段属性
      val loggerField =
        CtField.make("""private final LabzenLogger logger = Loggers.getLogger($className.class);""", clazz)
      clazz.addField(loggerField)

      // 生成注入Service相关的类属性字段
      val serviceHandlerAnnotation = interfaceType.getAnnotation(ServiceHandler::class.java)
      serviceHandlerAnnotation.main.apply {
        val ctField = createInjectWithResourceField(clazz, this)
        mainServiceFieldName = ctField.name
        services[ctField.name] = this.java
        clazz.addField(ctField)
      }
      serviceHandlerAnnotation.services.forEach {
        val ctField = createInjectWithResourceField(clazz, it)
        services[ctField.name] = it.java
        clazz.addField(ctField)
      }
    }
  }

  private fun implementInterfaceMethods() {
    with(controllerMeta) {
      val interfaceMethods = interfaceType.declaredMethods.filter {
        AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java)
      }
      interfaceMethods.forEach {
        methodGenerator.generateDeclaredInterfaceMethod(it)
      }
    }
  }

  private fun generateBaseResourceMethods() {
    with(controllerMeta) {
      if (interfaceType.isAnnotationPresent(BaseResource::class.java)) {
        val resourceAnnotation = interfaceType.getAnnotation(BaseResource::class.java)
        methodGenerator.generateBaseResourceMethods(resourceAnnotation)
      }
    }
  }

  private fun createDynamicClass() {
    val className = "${interfaceType.name}${configuration.controllerClassSuffix()}"
    val ctClass = classPool.makeClass(className)
    val ctClassFile = ctClass.classFile
    val ctConstPool = ctClassFile.constPool

    controllerMeta = ControllerMeta(configuration, interfaceType, className, ctClass, ctClassFile, ctConstPool)

    // 拷贝非 Labzen 注解到实现类上
    copyClassAnnotations()
    // 设置实现类的（注入）属性
    setClassFields()
    // 生成接口定义的方法实现
    implementInterfaceMethods()
    // 生成 BaseResource 的方法
    generateBaseResourceMethods()
  }

  // ========  Class  ==========

  // --------  Field  ----------

  /**
   * 生成使用 @Resource 注入的字段
   */
  private fun createInjectWithResourceField(ctClass: CtClass, fieldClass: KClass<*>): CtField {
    val ctFieldClass = classPool.getCtClass(fieldClass.qualifiedName)
    val fieldName = Strings.camelCase(fieldClass.simpleName!!)
    val ctField = CtField(ctFieldClass, fieldName, ctClass)
    ctField.modifiers = Modifier.PRIVATE

    with(controllerMeta) {
      val ctAnnotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
      ctAnnotationsAttribute.addAnnotation(CTAnnotation("javax.annotation.Resource", constPool))
      ctField.fieldInfo.addAttribute(ctAnnotationsAttribute)
    }
    return ctField
  }

  // ========  Field  ==========

  companion object {
    private val logger = logger { }

    private val defaultClassFileDirectory = System.getProperty("user.dir")
    private val classPool = ClassPool.getDefault().apply {
      importPackage("java.util")
      importPackage("cn.labzen.logger")
      importPackage("cn.labzen.logger.kernel")
      importPackage("cn.labzen.logger.kernel.enums")
      importPackage("cn.labzen.web.response.result.Result")
      importPackage("cn.labzen.web.request.PagingCondition")
    }
  }
}