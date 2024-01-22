package cn.labzen.web.source

import cn.labzen.web.annotation.BaseResource
import cn.labzen.web.source.methods.*
import java.lang.reflect.Method

internal class ControllerMethodGenerator(private val controllerMeta: ControllerMeta) {

  fun generateDeclaredInterfaceMethod(interfaceMethod: Method) {
    DeclaredInterfaceMethodGenerator(controllerMeta, interfaceMethod).generate()
  }

  fun generateBaseResourceMethods(resourceAnnotation: BaseResource) {
    // 调用ServiceHandler.main指定的Service
    val serviceFieldName = controllerMeta.mainServiceFieldName
    val serviceFieldClass: Class<*> = controllerMeta.services[serviceFieldName]!!

    with(resourceAnnotation) {
      val resourceBeanClass = resource.java

      BaseResourceCreateMethodGenerator(
        controllerMeta,
        methodCreate,
        serviceFieldClass,
        serviceFieldName,
        resourceBeanClass
      ).generate()

      BaseResourceRemoveMethodGenerator(
        controllerMeta,
        methodRemove,
        serviceFieldClass,
        serviceFieldName,
        resourceIdType.java,
        resourceId
      ).generate()

      BaseResourceEditMethodGenerator(
        controllerMeta,
        methodEdit,
        serviceFieldClass,
        serviceFieldName,
        resourceBeanClass
      ).generate()

      BaseResourceInfoMethodGenerator(
        controllerMeta,
        methodInfo,
        serviceFieldClass,
        serviceFieldName,
        resourceIdType.java,
        resourceId
      ).generate()

      BaseResourceAllMethodGenerator(
        controllerMeta,
        methodAll,
        serviceFieldClass,
        serviceFieldName
      ).generate()

      BaseResourceFindMethodGenerator(
        controllerMeta,
        methodEdit,
        serviceFieldClass,
        serviceFieldName,
        resourceBeanClass
      ).generate()
    }
  }
}