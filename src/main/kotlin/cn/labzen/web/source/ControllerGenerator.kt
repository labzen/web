package cn.labzen.web.source

import cn.labzen.cells.core.utils.Strings
import cn.labzen.logger.kernel.enums.Status
import cn.labzen.logger.kotlin.logger
import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.annotation.CallService
import cn.labzen.web.annotation.LabzenWeb
import cn.labzen.web.annotation.ServiceHandler
import cn.labzen.web.meta.WebConfiguration
import cn.labzen.web.request.PagingCondition
import com.google.common.collect.HashBiMap
import com.google.common.primitives.Primitives
import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ConstPool
import javassist.bytecode.MethodParametersAttribute
import javassist.bytecode.ParameterAnnotationsAttribute
import javassist.bytecode.annotation.*
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import javassist.bytecode.annotation.Annotation as CTAnnotation
import kotlin.Annotation as VMAnnotation

internal class ControllerGenerator(private val interfaceClass: Class<*>, private val configuration: WebConfiguration) {

  private val controllerClassName = "${interfaceClass.name}${configuration.controllerClassSuffix()}"
  private var mainServiceName: String = ""
  private val servicesInfo = HashBiMap.create<String, Class<*>>()

  fun generate(): Class<*> = createDynamicClass(controllerClassName, interfaceClass).toClass()

  private fun createDynamicClass(className: String, interfaceClass: Class<*>): CtClass {
    val ctClass = classPool.makeClass(className)
    val ctClassFile = ctClass.classFile
    val constPool = ctClassFile.constPool

    // 转换类的注解
    val interfaceAnnotations: Array<VMAnnotation> = interfaceClass.annotations.filter {
      // 对新生成的类，去除掉Labzen的注解
      !it.javaClass.isAnnotationPresent(LabzenWeb::class.java)
    }.toTypedArray()
    val ctClassAnnotationAttribute = duplicateAnnotations(interfaceAnnotations, constPool)
    ctClassFile.addAttribute(ctClassAnnotationAttribute)

    // 日志字段属性
    val ctLoggerField =
      CtField.make("""private final LabzenLogger logger = Loggers.getLogger($className.class);""", ctClass)
    ctClass.addField(ctLoggerField)

    // 生成注入Service相关的类属性字段
    if (interfaceClass.isAnnotationPresent(ServiceHandler::class.java)) {
      val serviceHandlerAnnotation = interfaceClass.getAnnotation(ServiceHandler::class.java)
      serviceHandlerAnnotation.main.apply {
        val ctField = createDynamicClassAutowiredField(ctClass, this, constPool)
        mainServiceName = ctField.name
        servicesInfo[ctField.name] = this.java
        ctClass.addField(ctField)
      }
      serviceHandlerAnnotation.services.forEach {
        val ctField = createDynamicClassAutowiredField(ctClass, it, constPool)
        servicesInfo[ctField.name] = it.java
        ctClass.addField(ctField)
      }
    }

    // 生成接口的方法实现
    val interfaceMethods = interfaceClass.declaredMethods.filter {
      AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java)
    }
    interfaceMethods.forEach {
      val ctMethod = createDynamicClassMethod(ctClass, it, constPool)
      ctClass.addMethod(ctMethod)
    }

    // 生成 BaseResource 的方法
    if (interfaceClass.isAnnotationPresent(BaseResource::class.java)) {
      val basedResourceMethods =
        createBasedResourceMethods(ctClass, interfaceClass.getAnnotation(BaseResource::class.java), constPool)
      basedResourceMethods.forEach { ctClass.addMethod(it) }
    }

    if (configuration.writeClassFile()) {
      val dir = if (Strings.isBlank(configuration.writeClassDirectory())) {
        defaultClassFileDirectory
      } else {
        configuration.writeClassDirectory()
      }
      ctClass.writeFile(dir)
      logger.info().status(Status.NOTE).scene("Controller")
        .log("Save dynamic mvc controller class [$className] to directory [$dir]")
    }
    return ctClass
  }

  private fun createDynamicClassAutowiredField(ctClass: CtClass, fieldClass: KClass<*>, constPool: ConstPool): CtField {
    val ctFieldClass = classPool.getCtClass(fieldClass.qualifiedName)
    val fieldName = fieldClass.simpleName!!.let { it.first().lowercaseChar() + it.substring(1) }
    val ctField = CtField(ctFieldClass, fieldName, ctClass)
    ctField.modifiers = Modifier.PRIVATE

    val ctAnnotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    val ctAnnotation = CTAnnotation("org.springframework.beans.factory.annotation.Autowired", constPool)
    ctAnnotationsAttribute.addAnnotation(ctAnnotation)
    ctField.fieldInfo.addAttribute(ctAnnotationsAttribute)

    return ctField
  }

  private fun createBasedResourceMethods(
    ctClass: CtClass,
    resourceAnnotation: BaseResource,
    constPool: ConstPool
  ): Set<CtMethod> {
    val createdMethods = mutableSetOf<CtMethod>()
    // 调用ServiceHandler.main指定的Service
    val serviceClass: Class<*>? = servicesInfo[mainServiceName]
    with(resourceAnnotation) {
      createResourceCreateMethod(ctClass, serviceClass, create, resource.java, constPool)
        .also { createdMethods.add(it) }
      createResourceRemoveMethod(ctClass, serviceClass, remove, resourceIdType.java, resourceId, constPool)
        .also { createdMethods.add(it) }
      createResourceEditMethod(ctClass, serviceClass, edit, resource.java, constPool)
        .also { createdMethods.add(it) }
      createResourceInfoMethod(ctClass, serviceClass, info, resourceIdType.java, resourceId, constPool)
        .also { createdMethods.add(it) }
      createResourceAllMethod(ctClass, serviceClass, find, constPool)
        .also { createdMethods.add(it) }
      createResourceFindMethod(ctClass, serviceClass, find, resource.java, constPool)
        .also { createdMethods.add(it) }
//      createResourceMethod(ctClass, export, resource.java, constPool).also { createdMethods.add(it) }
    }
    return createdMethods
  }

  @Suppress("DuplicatedCode")
  private fun createResourceCreateMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    resourceClass: Class<*>,
    constPool: ConstPool
  ): CtMethod {
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name, resourceClass)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的新增接口，需要一个 [public Result $name(${resourceClass.simpleName} resource)] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name(resource);
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result createResource(${resourceClass.name} resource) {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置方法参数名
    val parameterAttribute = MethodParametersAttribute(constPool, arrayOf("resource"), intArrayOf(0))
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 一个参数，Resource Bean，加注解 @Validated @RequestBody
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>>()
    ctParameterAnnotations.add(
      arrayOf(
        CTAnnotation("org.springframework.validation.annotation.Validated", constPool),
        CTAnnotation("org.springframework.web.bind.annotation.ModelAttribute", constPool)
//        CTAnnotation("org.springframework.web.bind.annotation.RequestBody", constPool)
      )
    )
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    annotationsAttribute.addAnnotation(CTAnnotation("org.springframework.web.bind.annotation.PostMapping", constPool))
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  @Suppress("DuplicatedCode")
  private fun createResourceRemoveMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    resourceIdClass: Class<*>,
    resourceIdName: String,
    constPool: ConstPool
  ): CtMethod {
    val parameterClass = if (resourceIdClass.isPrimitive) Primitives.wrap(resourceIdClass) else resourceIdClass
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name, parameterClass)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的删除接口，需要一个 [public Result $name(${parameterClass.simpleName} $resourceIdName)] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name($resourceIdName);
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result removeResource(${parameterClass.name} $resourceIdName) {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置方法参数名
    val parameterAttribute = MethodParametersAttribute(constPool, arrayOf(resourceIdName), intArrayOf(0))
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 一个参数，Resource Bean，加注解 @PathVariable
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>>()
    ctParameterAnnotations.add(
      arrayOf(CTAnnotation("org.springframework.web.bind.annotation.PathVariable", constPool))
    )
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    val mappingAnnotation = CTAnnotation("org.springframework.web.bind.annotation.DeleteMapping", constPool)
    val mappingAnnotationMember =
      ArrayMemberValue(constPool).apply { value = arrayOf(StringMemberValue("/{$resourceIdName}", constPool)) }
    mappingAnnotation.addMemberValue("value", mappingAnnotationMember)
    annotationsAttribute.addAnnotation(mappingAnnotation)
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  @Suppress("DuplicatedCode")
  private fun createResourceEditMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    resourceClass: Class<*>,
    constPool: ConstPool
  ): CtMethod {
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name, resourceClass)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的修改接口，需要一个 [public Result $name(${resourceClass.simpleName} resource)] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name(resource);
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result editResource(${resourceClass.name} resource) {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置方法参数名
    val parameterAttribute = MethodParametersAttribute(constPool, arrayOf("resource"), intArrayOf(0))
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 一个参数，Resource Bean，加注解 @Validated @RequestBody
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>>()
    ctParameterAnnotations.add(
      arrayOf(
        CTAnnotation("org.springframework.validation.annotation.Validated", constPool),
        CTAnnotation("org.springframework.web.bind.annotation.ModelAttribute", constPool)
//        CTAnnotation("org.springframework.web.bind.annotation.RequestBody", constPool)
      )
    )
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    annotationsAttribute.addAnnotation(CTAnnotation("org.springframework.web.bind.annotation.PutMapping", constPool))
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  @Suppress("DuplicatedCode")
  private fun createResourceInfoMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    resourceIdClass: Class<*>,
    resourceIdName: String,
    constPool: ConstPool
  ): CtMethod {
    val parameterClass = if (resourceIdClass.isPrimitive) Primitives.wrap(resourceIdClass) else resourceIdClass
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name, parameterClass)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的资源信息接口，需要一个 [public Result $name(${parameterClass.simpleName} $resourceIdName)] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name($resourceIdName);
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result resourceInfo(${parameterClass.name} $resourceIdName) {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置方法参数名
    val parameterAttribute = MethodParametersAttribute(constPool, arrayOf(resourceIdName), intArrayOf(0))
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 一个参数，Resource Bean，加注解 @PathVariable
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>>()
    ctParameterAnnotations.add(
      arrayOf(CTAnnotation("org.springframework.web.bind.annotation.PathVariable", constPool))
    )
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    val mappingAnnotation = CTAnnotation("org.springframework.web.bind.annotation.GetMapping", constPool)
    val mappingAnnotationMember =
      ArrayMemberValue(constPool).apply { value = arrayOf(StringMemberValue("/{$resourceIdName}", constPool)) }
    mappingAnnotation.addMemberValue("value", mappingAnnotationMember)
    annotationsAttribute.addAnnotation(mappingAnnotation)
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  private fun createResourceAllMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    constPool: ConstPool
  ): CtMethod {
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的查找接口，需要一个 [public Result $name()] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name();
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result allResources() {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    val mappingAnnotation = CTAnnotation("org.springframework.web.bind.annotation.GetMapping", constPool)
    annotationsAttribute.addAnnotation(mappingAnnotation)
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  private fun createResourceFindMethod(
    ctClass: CtClass,
    callServiceClass: Class<*>?,
    name: String,
    resourceClass: Class<*>,
    constPool: ConstPool
  ): CtMethod {
    val methodBodyText = if (callServiceClass == null) {
      """
        logger.warn().status(Status.FIXME).log("BaseResource注解依赖ServiceHandler.main来指定需要调用的Service Bean");
        return null;
      """.trimIndent()
    } else {
      val callMethod = try {
        callServiceClass.getDeclaredMethod(name, resourceClass, PagingCondition::class.java)
      } catch (e: NoSuchMethodException) {
        null
      }
      if (callMethod == null) {
        """
          logger.warn().status(Status.FIXME).log("BaseResource的查找接口，需要一个 [public Result $name(${resourceClass.simpleName} resource, PagingCondition pageCondition)] Service方法");
          return null;
        """.trimIndent()
      } else {
        """
       		return ${servicesInfo.inverse()[callServiceClass]}.$name(resourceCondition, pageCondition);
     		""".trimIndent()
      }
    }
    val methodDeclaration = """
  		public Result findResources(${resourceClass.name} resourceCondition, PagingCondition pageCondition) {
  			$methodBodyText
  		}
		""".trimIndent()
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置返回值的泛型
//    val cs = SignatureAttribute.ClassSignature(arrayOf(SignatureAttribute.TypeParameter(resourceClass.name)))
//    ctMethod.returnType.genericSignature = cs.encode()
    // 设置方法参数名
    val ctParameterAttribute =
      MethodParametersAttribute(constPool, arrayOf("resourceCondition", "pageCondition"), intArrayOf(0, 0))
    ctMethod.methodInfo.addAttribute(ctParameterAttribute)

    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>?>()
    val modelAttributeAnnotation =
      CTAnnotation("org.springframework.web.bind.annotation.ModelAttribute", constPool)
    ctParameterAnnotations.add(arrayOf(modelAttributeAnnotation))
    ctParameterAnnotations.add(arrayOf(modelAttributeAnnotation))
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    val mappingAnnotation = CTAnnotation("org.springframework.web.bind.annotation.GetMapping", constPool)
    val mappingAnnotationMember =
      ArrayMemberValue(constPool).apply {
        value = arrayOf(StringMemberValue("/find", constPool))
      }
    mappingAnnotation.addMemberValue("value", mappingAnnotationMember)
    annotationsAttribute.addAnnotation(mappingAnnotation)
    ctMethod.methodInfo.addAttribute(annotationsAttribute)

    return ctMethod
  }

  private fun createDynamicClassMethod(ctClass: CtClass, interfaceMethod: Method, constPool: ConstPool): CtMethod {
    // 创建方法
    val methodDeclaration = generateMethodDeclaration(interfaceMethod)
    val ctMethod = CtNewMethod.make(methodDeclaration, ctClass)
    // 设置方法参数名
    val parameterNames: Array<String> = interfaceMethod.parameters.map { it.name }.toTypedArray()
    val parameterAttribute =
      MethodParametersAttribute(constPool, parameterNames, IntArray(parameterNames.size) { 0 })
    ctMethod.methodInfo.addAttribute(parameterAttribute)

    // 转换方法参数的注解
    val ctParameterAnnotationsAttribute = duplicateParametersAnnotations(interfaceMethod.parameters, constPool)
    ctMethod.methodInfo.addAttribute(ctParameterAnnotationsAttribute)

    // 转换方法的注解
    val interfaceMethodAnnotations: Array<VMAnnotation> = interfaceMethod.annotations.filter {
      // 对新生成的方法实现，去除掉Labzen的注解
      !it.javaClass.isAnnotationPresent(LabzenWeb::class.java)
    }.toTypedArray()
    val ctClassAnnotationAttribute = duplicateAnnotations(interfaceMethodAnnotations, constPool)
    ctMethod.methodInfo.addAttribute(ctClassAnnotationAttribute)

    return ctMethod
  }

  private fun generateMethodDeclaration(interfaceMethod: Method): String {
    val body = generateMethodBodyText(interfaceMethod)

    val methodParameterNames = interfaceMethod.parameters.joinToString(",") {
      "${it.type.name} ${it.name}"
    }
    return """
      public ${interfaceMethod.returnType.name} ${interfaceMethod.name}($methodParameterNames) {
				$body
      }
    """.trimIndent()
  }

  private fun generateMethodBodyText(interfaceMethod: Method): String {
    // 默认调用与controller相同方法名的service方法
    var callMethodName = interfaceMethod.name
    var callServiceClass: Class<*>? = servicesInfo[mainServiceName]
    if (interfaceMethod.isAnnotationPresent(CallService::class.java)) {
      val csAnnotation = interfaceMethod.getAnnotation(CallService::class.java)
      if (csAnnotation.method.isNotBlank()) {
        callMethodName = csAnnotation.method
      }
      if (csAnnotation.handler != Any::class) {
        callServiceClass = csAnnotation.handler.java
      }
    }

    val controllerReturnType = interfaceMethod.returnType
    val voidControllerReturn = controllerReturnType == Void::class.java

    val controllerReturnStatement = if (voidControllerReturn) "" else "return null;"
    if (mainServiceName.isBlank()) {
      return """
        logger.warn().status(Status.FIXME).log("没有Controller的方法实现，未指定ServiceHandler注解的main参数");
        $controllerReturnStatement
        """.trimIndent()
    }

    if (!servicesInfo.containsValue(callServiceClass!!)) {
      return """logger.warn().status(Status.FIXME).log("未在ServiceHandler注解中指定Service - [${callServiceClass.name}]");
        $controllerReturnStatement
      """.trimMargin()
    }

    val serviceMethod = try {
      callServiceClass.getDeclaredMethod(callMethodName, *interfaceMethod.parameterTypes)
    } catch (e: NoSuchMethodException) {
      return """logger.warn().status(Status.FIXME).log("在Service - [${callServiceClass.name}] 中找不到可调用的方法 - [$callMethodName]");
        $controllerReturnStatement
        """.trimIndent()
    }

    val serviceFieldName = servicesInfo.inverse()[callServiceClass]

    val serviceReturnType = serviceMethod.returnType
    return if (controllerReturnType == Void::class.java) {
      """logger.warn().status(Status.REMIND).log("controller方法 - [${interfaceMethod.name}] 未提供返回值");
        $controllerReturnStatement
      """.trimIndent()
    } else if (serviceReturnType == Void::class.java) {
      """logger.warn().status(Status.REMIND).log("service方法 - [$callMethodName] 未提供返回值");
        $controllerReturnStatement
      """.trimIndent()
    } else if (controllerReturnType != serviceReturnType) {
      """logger.warn().status(Status.REMIND).log("controller调用service的方法返回值不一致 - [$controllerReturnType, $serviceReturnType]");
        $controllerReturnStatement
      """.trimIndent()
    } else {
      """
    		return $serviceFieldName.$callMethodName($$);
      """.trimIndent()
    }
  }

  private fun duplicateAnnotations(
    interfaceAnnotations: Array<VMAnnotation>,
    constPool: ConstPool
  ): AnnotationsAttribute {
    val ctAnnotations = interfaceAnnotations.map { ia -> duplicateAnnotation(ia, constPool) }

    val annotationsAttribute = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
    ctAnnotations.forEach { annotationsAttribute.addAnnotation(it) }
    return annotationsAttribute
  }

  private fun duplicateParametersAnnotations(
    parameters: Array<Parameter>, constPool: ConstPool
  ): ParameterAnnotationsAttribute {
    val ctParameterAnnotationsAttribute =
      ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag)
    val ctParameterAnnotations = mutableListOf<Array<CTAnnotation>>()
    parameters.forEach { param ->
      val ctAnnotations = param.annotations.map { ia -> duplicateAnnotation(ia, constPool) }
      ctParameterAnnotations.add(ctAnnotations.toTypedArray())
    }
    ctParameterAnnotationsAttribute.annotations = ctParameterAnnotations.toTypedArray()
    return ctParameterAnnotationsAttribute
  }

  private fun duplicateAnnotation(interfaceAnnotation: VMAnnotation, constPool: ConstPool): CTAnnotation {
    val iaClass = interfaceAnnotation.annotationClass
    val ctAnnotation = CTAnnotation(iaClass.qualifiedName, constPool)

    val iaMembers = iaClass.declaredMembers
    iaMembers.forEach { iam ->
      val value = iam.call(interfaceAnnotation)
      val ctAnnotationMemberValue: MemberValue? = value?.let { parseAnnotationMemberValue(it, constPool) }
      ctAnnotationMemberValue?.apply {
        ctAnnotation.addMemberValue(iam.name, ctAnnotationMemberValue)
      }
    }
    return ctAnnotation
  }

  private fun parseAnnotationMemberValue(value: Any, constPool: ConstPool): MemberValue {
    return when (value) {
      is String -> StringMemberValue(value, constPool)
      is Int -> IntegerMemberValue(value, constPool)
      is Short -> ShortMemberValue(value, constPool)
      is Long -> LongMemberValue(value, constPool)
      is Boolean -> BooleanMemberValue(value, constPool)
      is Byte -> ByteMemberValue(value, constPool)
      is Double -> DoubleMemberValue(value, constPool)
      is Float -> FloatMemberValue(value, constPool)
      is Char -> CharMemberValue(value, constPool)

      is Enum<*> -> EnumMemberValue(constPool).apply {
        this.type = value.javaClass.name
        this.value = value.name
      }

      is Class<*> -> ClassMemberValue(value.name, constPool)

      is VMAnnotation -> AnnotationMemberValue(duplicateAnnotation(value, constPool), constPool)

      is Array<*> -> {
        val arrayMemberValue = ArrayMemberValue(constPool)
        arrayMemberValue.value = value.mapNotNull {
          if (it != null) {
            parseAnnotationMemberValue(it, constPool)
          } else null
        }.toTypedArray()
        return arrayMemberValue
      }

      else -> throw IllegalArgumentException("不支持的注解参数 - ${value.javaClass}")
    }
  }

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