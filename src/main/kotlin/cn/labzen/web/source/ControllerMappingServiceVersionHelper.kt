package cn.labzen.web.source

import cn.labzen.web.meta.RequestMappingVersionPlace
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.IntegerMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping

internal object ControllerMappingServiceVersionHelper {

  fun setupMappedRequestVersion(
    controllerMeta: ControllerMeta,
    mappedServiceVersion: Int?,
    annotations: MutableList<Annotation>
  ) {
    val configuration = controllerMeta.configuration
    if (!configuration.controllerVersionEnabled()) {
      return
    }

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
      val version = configuration.controllerVersionPrefix() + (mappedServiceVersion ?: 1)

      when (configuration.controllerVersionPlace()) {
        RequestMappingVersionPlace.HEAD -> setupVersionInHead(controllerMeta, it, version)
        RequestMappingVersionPlace.URI -> {
          // do nothing
        }

        RequestMappingVersionPlace.PARAM -> setupVersionInParam(controllerMeta, it, version)
      }
    }
  }

  private fun setupVersionInURI(controllerMeta: ControllerMeta, version: Int): Annotation {
    val constPool = controllerMeta.constPool
    val versionAnnotation = Annotation("cn.labzen.web.annotation.MappingApiVersion", constPool)
//    val versionAnnotationMember =
//      ArrayMemberValue(constPool).apply {
//        value = arrayOf()
//      }
    versionAnnotation.addMemberValue("value", IntegerMemberValue(constPool, version))
    return versionAnnotation
  }

  private fun setupVersionInHead(controllerMeta: ControllerMeta, annotation: Annotation, version: String) {
    val headMemberValue = "application/vnd.${controllerMeta.configuration.controllerVersionVNDName()}.$version+json"

    val memberValue: ArrayMemberValue =
      (annotation.getMemberValue("produces") ?: ArrayMemberValue(controllerMeta.constPool)) as ArrayMemberValue
    val appendHeadString = arrayOf(StringMemberValue(headMemberValue, controllerMeta.constPool))
    memberValue.value = memberValue.value?.let { it + appendHeadString } ?: appendHeadString
    annotation.addMemberValue("produces", memberValue)
  }

  private fun setupVersionInParam(controllerMeta: ControllerMeta, annotation: Annotation, version: String) {
    val paramsMemberValue = "${controllerMeta.configuration.controllerVersionParamName()}=$version"

    val memberValue: ArrayMemberValue =
      (annotation.getMemberValue("params") ?: ArrayMemberValue(controllerMeta.constPool)) as ArrayMemberValue
    val appendParamsString = arrayOf(StringMemberValue(paramsMemberValue, controllerMeta.constPool))
    memberValue.value = memberValue.value?.let { it + appendParamsString } ?: appendParamsString
    annotation.addMemberValue("params", memberValue)
  }
}