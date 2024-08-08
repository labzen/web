package cn.labzen.web.source

import cn.labzen.tool.util.Strings
import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.LOGGER_SCENE_CONTROLLER
import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.annotation.CallService
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
 * Controller接口的实现类生成器
 */
internal class ControllerGenerator(private val configuration: WebConfiguration, private val interfaceType: Class<*>) {

  private lateinit var controllerMeta: ControllerMeta

  /**
   * 生成类
   */
  fun generate(): Class<*> {
    createDynamicClass()

    saveFileIfNecessary()

    return controllerMeta.clazz.toClass()
  }

  /**
   * 保存类文件
   *
   * todo 生成的文件路径，调整一下，在开发环境中，最好是与 target 下的 generated-sources（生产环境再考虑）
   */
  private fun saveFileIfNecessary() {
    if (configuration.saveClassFile()) {
      val dir = if (Strings.isBlank(configuration.writeClassDirectoryTo())) {
        defaultClassFileDirectory
      } else {
        configuration.writeClassDirectoryTo()
      }
      controllerMeta.clazz.writeFile(dir)
      logger.info().status(Status.NOTE).scene(LOGGER_SCENE_CONTROLLER)
        .log("保存Controller实现类 [${controllerMeta.className}] 在 [$dir]")
    }
  }

  // ----------------  Class  ----------------

  /**
   * 将Controller接口上的所有注解，原样拷贝到实现类上（排除掉Labzen的注解）
   */
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

  /**
   * 为Controller实现类加入必须得私有属性
   */
  private fun setClassFields() {
    with(controllerMeta) {
      // 日志属性 logger
      val loggerField =
        CtField.make("""private final LabzenLogger logger = Loggers.getLogger($className.class);""", clazz)
      clazz.addField(loggerField)

      // 需要用@Resource注入的Service属性
      // ServiceHandler注解是肯定存在的（ControllerClassInitializer开始扫描Controller接口时，这是必须条件）
      val serviceHandlerAnnotation = interfaceType.getAnnotation(ServiceHandler::class.java)
      serviceHandlerAnnotation.value.apply {
        val ctField = createFieldWithResourceInject(clazz, this)
        mainServiceFieldName = ctField.name
        services[ctField.name] = this.java
        clazz.addField(ctField)
      }

      // 从Controller接口中定义的所有方法上，找额外需要调用的Service类
      val otherServices = interfaceType.declaredMethods
        .filter { it.isAnnotationPresent(CallService::class.java) }
        .mapNotNull {
          when (val handler = it.getAnnotation(CallService::class.java).handler) {
            Any::class -> null
            serviceHandlerAnnotation.value -> null
            else -> handler
          }
        }
      otherServices.forEach {
        val ctField = createFieldWithResourceInject(clazz, it)
        services[ctField.name] = it.java
        clazz.addField(ctField)
      }
    }
  }

  /**
   * Controller接口原生代码中定义过的方法，添加到实现类中；仅处理注解了[RequestMapping]的方法
   */
  private fun implementInterfaceMethods() {
    with(controllerMeta) {
      val interfaceMethods = interfaceType.declaredMethods.filter {
        AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java)
      }
      interfaceMethods.forEach {
        // 通过方法生成器来实现
        methodGenerator.generateDeclaredInterfaceMethod(it)
      }
    }
  }

  /**
   * 根据Controller接口上，如果注解了[BaseResource]，则添加与指定 Resource Bean 相关的常见操作API方法
   */
  private fun generateBaseResourceMethods() {
    with(controllerMeta) {
      if (interfaceType.isAnnotationPresent(BaseResource::class.java)) {
        val resourceAnnotation = interfaceType.getAnnotation(BaseResource::class.java)
        // 通过方法生成器来实现
        methodGenerator.generateBaseResourceMethods(resourceAnnotation)
      }
    }
  }

  /**
   * 生成类的主入口
   */
  private fun createDynamicClass() {
    // 默认Controller类名是 "接口名 + 配置的后缀（默认Impl）"
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

  // ================  Class  ==================

  // ----------------  Field  --------------------

  /**
   * 生成Controller的属性，使用 @Resource 注入
   *
   * @param fieldClass Service类
   */
  private fun createFieldWithResourceInject(ctClass: CtClass, fieldClass: KClass<*>): CtField {
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

  // ================  Field  ==================

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