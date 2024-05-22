package cn.labzen.web.source

import cn.labzen.web.meta.RequestMappingVersionPlace
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.IntegerMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping

internal object ControllerMappingVersionHelper {

  /**
   * 设置API版本控制所需要在方法上设置的注解等
   */
  fun setupMappedRequestVersion(
    controllerMeta: ControllerMeta,
    mappedServiceVersion: Int?,
    annotations: MutableList<Annotation>
  ) {
    val configuration = controllerMeta.configuration
    if (!configuration.controllerVersionEnabled()) {
      return
    }

    // 如果是通过 URI 来控制版本
    if (configuration.controllerVersionPlace() == RequestMappingVersionPlace.URI) {
      val versionAnnotation = setupVersionInURI(controllerMeta, mappedServiceVersion ?: 1)
      annotations.add(versionAnnotation)
      return
    }

    annotations.firstOrNull {
      val annotationClass = Class.forName(it.typeName)
      annotationClass == RequestMapping::class.java ||
        AnnotatedElementUtils.isAnnotated(annotationClass, RequestMapping::class.java)
    }?.let {
      val version = configuration.controllerVersionPrefix() + (mappedServiceVersion ?: controllerMeta.apiVersion)

      when (configuration.controllerVersionPlace()) {
        // 如果通过 Header 中的 Accept 控制API版本
        RequestMappingVersionPlace.HEADER -> setupVersionInHeader(controllerMeta, it, version)
        // 如果通过每次API请求携带的参数控制版本
        RequestMappingVersionPlace.PARAM -> setupVersionInParam(controllerMeta, it, version)
        RequestMappingVersionPlace.URI -> {
          // do nothing
        }
      }
    }
  }

  /**
   * 设置 URI 版本控制
   */
  private fun setupVersionInURI(controllerMeta: ControllerMeta, version: Int): Annotation {
    val constPool = controllerMeta.constPool
    val versionAnnotation = Annotation("cn.labzen.web.annotation.runtime.APIVersion", constPool)
    versionAnnotation.addMemberValue("value", IntegerMemberValue(constPool, version))
    return versionAnnotation
  }

  /**
   * 设置 Header Accept 版本控制
   */
  private fun setupVersionInHeader(controllerMeta: ControllerMeta, annotation: Annotation, version: String) {
    val headerVersionValue = "application/vnd.${controllerMeta.configuration.controllerVersionVNDName()}.$version+json"

    val producesMember: ArrayMemberValue =
      (annotation.getMemberValue("produces") ?: ArrayMemberValue(controllerMeta.constPool)) as ArrayMemberValue
    val producesMemberValues = arrayOf(StringMemberValue(headerVersionValue, controllerMeta.constPool))
    producesMember.value = producesMember.value?.let { it + producesMemberValues } ?: producesMemberValues
    annotation.addMemberValue("produces", producesMember)
  }

  /**
   * 设置请求参数控制版本
   */
  private fun setupVersionInParam(controllerMeta: ControllerMeta, annotation: Annotation, version: String) {
    val paramsMemberValue = "${controllerMeta.configuration.controllerVersionParamName()}=$version"

    val memberValue: ArrayMemberValue =
      (annotation.getMemberValue("params") ?: ArrayMemberValue(controllerMeta.constPool)) as ArrayMemberValue
    val appendParamsString = arrayOf(StringMemberValue(paramsMemberValue, controllerMeta.constPool))
    memberValue.value = memberValue.value?.let { it + appendParamsString } ?: appendParamsString
    annotation.addMemberValue("params", memberValue)
  }
}