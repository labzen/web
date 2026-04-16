package cn.labzen.web.apt.config;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Properties;

import static cn.labzen.web.apt.definition.JUnitConstants.JUNIT_OUTPUT_DIR;

/**
 * APT 处理器配置加载器
 * <p>
 * 负责从文件系统加载 labzen.web.config 配置文件。
 * 配置加载顺序为：模块级配置 -> 项目根配置（后者仅补充前者未设置的项）。
 */
public final class ConfigLoader {

  private static final String CONFIG_FILE_NAME = "labzen.web.config";

  private ConfigLoader() {
  }

  /**
   * 加载配置文件
   * <p>
   * 加载顺序：
   * 1. 先加载当前模块根目录的 labzen.web.config
   * 2. 再加载 Maven 根目录的 labzen.web.config（仅补充未设置的项）
   *
   * @param filer 文件写入器，用于探测当前模块路径
   * @return 配置对象
   */
  public static Config load(Filer filer) {
    var merged = new Properties();

    var moduleDir = findModuleRootDir(filer);
    var moduleConfig = new File(moduleDir, CONFIG_FILE_NAME);
    if (moduleConfig.exists()) {
      try (InputStream is = moduleConfig.toURI().toURL().openStream()) {
        merged.load(is);
      } catch (IOException e) {
        throw new RuntimeException("Failed to load module config: " + moduleConfig.getAbsolutePath(), e);
      }
    }

    var rootDir = findMavenRootDir(moduleDir);
    if (!rootDir.equals(moduleDir)) {
      var rootConfig = new File(rootDir, CONFIG_FILE_NAME);
      if (rootConfig.exists()) {
        var rootProps = new Properties();
        try (InputStream is = rootConfig.toURI().toURL().openStream()) {
          rootProps.load(is);
          for (var entry : rootProps.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to load root config: " + rootConfig.getAbsolutePath(), e);
        }
      }
    }

    return new Config(merged);
  }

  /**
   * 查找当前模块的根目录
   * <p>
   * 通过 Filer 创建探测文件来获取当前编译输出路径，
   * 然后向上遍历查找包含 pom.xml 的目录作为模块根目录。
   * 兼容 JUnit 测试环境。
   *
   * @param filer 文件写入器
   * @return 模块根目录
   */
  private static File findModuleRootDir(Filer filer) {
    // 这里兼容一下JUNIT环境
    String outputTarget = System.getProperty(JUNIT_OUTPUT_DIR, "");
    URI resource;
    if (outputTarget.trim().isEmpty()) {
      try {
        // ___probe___为虚拟文件名，不会真正写内容
        FileObject probe = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "___probe___");
        resource = probe.toUri();
      } catch (IOException e) {
        throw new RuntimeException("Failed to create probe resource", e);
      }
    } else {
      resource = Paths.get(System.getProperty("buildDir", "target/classes")).toUri();
    }

    var currentDir = new File(resource).getParentFile();
    while (currentDir != null) {
      var pomFile = new File(currentDir, "pom.xml");
      if (pomFile.exists()) {
        return currentDir;
      }
      currentDir = currentDir.getParentFile();
    }

    throw new IllegalStateException("无法找到当前模块根目录（无 pom.xml）");
  }

  /**
   * 查找 Maven 项目的根目录
   * <p>
   * 从指定目录向上遍历，找到最顶层的包含 pom.xml 的目录。
   *
   * @param fromDir 起始目录
   * @return Maven 根目录
   */
  private static File findMavenRootDir(File fromDir) {
    var current = fromDir;
    File lastPomDir = null;

    while (current != null) {
      var pomFile = new File(current, "pom.xml");
      if (pomFile.exists()) {
        lastPomDir = current;
      }
      current = current.getParentFile();
    }

    return (lastPomDir != null) ? lastPomDir : fromDir;
  }
}
