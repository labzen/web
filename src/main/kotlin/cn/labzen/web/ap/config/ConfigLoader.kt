package cn.labzen.web.ap.config

import cn.labzen.web.JUNIT_OUTPUT_DIR
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.annotation.processing.Filer
import javax.tools.StandardLocation

object ConfigLoader {

  private const val CONFIG_FILE_NAME = "labzen.web.config"

  fun load(filer: Filer): Config {
    val merged = Properties()

    val moduleDir = findModuleRootDir(filer)
    val moduleConfig = File(moduleDir, CONFIG_FILE_NAME)
    if (moduleConfig.exists()) {
      moduleConfig.inputStream().use { merged.load(it) }
    }

    val rootDir = findMavenRootDir(moduleDir)
    if (rootDir != moduleDir) {
      val rootConfig = File(rootDir, CONFIG_FILE_NAME)
      if (rootConfig.exists()) {
        val rootProps = Properties()
        rootConfig.inputStream().use { rootProps.load(it) }
        for ((k, v) in rootProps) {
          merged.putIfAbsent(k, v)
        }
      }
    }

    return Config(merged)
  }

  private fun findModuleRootDir(filer: Filer): File {
    // 这里兼容一下JUNIT环境
    val outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "")
    val resource = if (outputTarget.isNotBlank()) {
      Paths.get(System.getProperty("buildDir", "target/classes")).toUri()
    } else {
      // ___probe___为虚拟文件名，不会真正写内容
      filer.createResource(StandardLocation.CLASS_OUTPUT, "", "___probe___").toUri()
    }

    val outputDir = File(resource).parentFile

    return generateSequence(outputDir) { it.parentFile }
      .firstOrNull { File(it, "pom.xml").exists() }
      ?: error("无法找到当前模块根目录（无 pom.xml）")
  }

  private fun findMavenRootDir(fromDir: File): File {
    var current: File? = fromDir
    var lastPomDir: File? = null

    while (current != null) {
      if (File(current, "pom.xml").exists()) {
        lastPomDir = current
      }
      current = current.parentFile
    }

    return lastPomDir ?: fromDir
  }
}