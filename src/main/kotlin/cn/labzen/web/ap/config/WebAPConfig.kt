package cn.labzen.web.ap.config

import cn.labzen.web.JUNIT_OUTPUT_DIR
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_BASED
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_CARRIER
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_HEADER_VND
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_PARAMETER_NAME
import cn.labzen.web.ap.config.ConfigKeys.API_VERSION_PREFIX
import cn.labzen.web.ap.config.ConfigKeys.CLASS_NAME_SUFFIX
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_BASED_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_CARRIER_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_HEADER_VND_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_PARAMETER_NAME_VALUE
import cn.labzen.web.ap.config.ConfigValues.API_VERSION_PREFIX_VALUE
import cn.labzen.web.ap.config.ConfigValues.CLASS_NAME_SUFFIX_VALUE
import cn.labzen.web.defination.APIVersionCarrier
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.annotation.processing.Filer
import javax.tools.StandardLocation

class WebAPConfig(filer: Filer) {

  private val properties = Properties()

  init {
    val outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "")
    val resource = if (outputTarget.isNotBlank()) {
      Paths.get(System.getProperty("buildDir", "target/classes")).toUri()
    } else {
      // ___probe___为虚拟文件名，不会真正写内容
      filer.createResource(StandardLocation.CLASS_OUTPUT, "", "___probe___").toUri()
    }
    val classPathFolder = File(resource)

    // 获取到项目的根路径
    val projectRoot = findProjectRoot(classPathFolder)
    projectRoot ?: println("找不到项目根路径")

    val aptConfig = File(projectRoot, FILE_NAME)
    if (aptConfig.exists()) {
      readConfigFile(aptConfig)
    }
//    val configFiles = mutableListOf<File>()
//    while (
//      projectRoot != null &&
//      classPathFolder != null &&
//      classPathFolder.absolutePath.length >= projectRoot.absolutePath.length
//    ) {
//      val aptConfig = File(projectRoot, FILE_NAME)
//      if (aptConfig.exists()) {
//        configFiles += aptConfig
//      }
//      classPathFolder = classPathFolder.parentFile
//    }
//
//    configFiles.forEach(::readConfigFile)
  }

  private fun readConfigFile(file: File) {
    val config = Properties()
    file.inputStream().use {
      config.load(it)
    }

    config.forEach { k, v ->
      properties.computeIfAbsent(k) { v.toString() }
    }
  }

  private fun findProjectRoot(startingPath: File): File? {
    var current = startingPath.absoluteFile
    while (current.parentFile != null) {
      if (current.isDirectory && buildFileNames.any { File(current, it).exists() }) {
        return current
      }
      current = current.parentFile
    }
    return null
  }

  fun classNameSuffix(): String {
    val original = properties.getOrDefault(CLASS_NAME_SUFFIX, CLASS_NAME_SUFFIX_VALUE).toString()
    return original.ifBlank { CLASS_NAME_SUFFIX_VALUE }
  }

  fun apiVersionCarrier(): APIVersionCarrier {
    val carrier = properties.getOrDefault(API_VERSION_CARRIER, API_VERSION_CARRIER_VALUE).toString()
    return try {
      APIVersionCarrier.valueOf(carrier)
    } catch (e: Exception) {
      println("未配置有效的 processor.api-version.carrier ，将使用 DISABLE，生成的 Controller 实现类将禁用API版本控制能力")
      APIVersionCarrier.DISABLE
    }
  }

  fun apiVersionPrefix(): String {
    val original = properties.getOrDefault(API_VERSION_PREFIX, API_VERSION_PREFIX_VALUE).toString()
    return original.ifBlank { API_VERSION_PREFIX_VALUE }
  }

  fun apiVersionBased(): Int {
    val based = properties.getOrDefault(API_VERSION_BASED, API_VERSION_BASED_VALUE).toString()
    return based.toIntOrNull() ?: let {
      println("未配置有效的 processor.api-version.based ，默认使用 1")
      1
    }
  }

  fun apiVersionHeaderVND(): String {
    val original = properties.getOrDefault(API_VERSION_HEADER_VND, API_VERSION_HEADER_VND_VALUE).toString()
    return original.ifBlank { API_VERSION_HEADER_VND_VALUE }
  }

  fun apiVersionParameterName(): String {
    val original = properties.getOrDefault(API_VERSION_PARAMETER_NAME, API_VERSION_PARAMETER_NAME_VALUE).toString()
    return original.ifBlank { API_VERSION_PARAMETER_NAME_VALUE }
  }

  companion object {

    private const val FILE_NAME = "labzen.web.config"
    private val buildFileNames = listOf("pom.xml", "build.gradle", "build.gradle.kts")
  }
}