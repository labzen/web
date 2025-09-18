package cn.labzen.web.ap.config;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Properties;

import static cn.labzen.web.defination.Constants.JUNIT_OUTPUT_DIR;

public final class ConfigLoader {

  private static final String CONFIG_FILE_NAME = "labzen.web.config";

  private ConfigLoader() {
  }

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
