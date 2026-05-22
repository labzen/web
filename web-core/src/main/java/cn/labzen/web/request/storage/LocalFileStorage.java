package cn.labzen.web.request.storage;

import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.request.FileStorage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件系统存储实现。
 * <p>
 * 支持配置目录组织粒度：
 * <ul>
 *   <li>{@code NONE} - 所有文件存储在根路径下</li>
 *   <li>{@code YMD} - 按日期一级目录组织，如 {@code 2026-05-14/}</li>
 *   <li>{@code YM_D} - 按年-月/日二级目录组织，如 {@code 2026-05/14/}</li>
 *   <li>{@code Y_M} - 按年/月二级目录组织，如 {@code 2026/05/}</li>
 *   <li>{@code Y_M_D} - 按年/月/日三级目录组织，如 {@code 2026/05/14/}</li>
 * </ul>
 * <p>
 * 初始化参数为 JSON 格式字符串，示例：
 * <pre>
 * {"root":"/data/uploads","granularity":"YMD"}
 * </pre>
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

  private static final DateTimeFormatter YMD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter YM_D_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private Path rootPath;
  private StorageGranularity granularity;

  @Override
  public boolean initialize(JsonNode config) {
    String root = config.get("root").asText();
    this.rootPath = Path.of(root).normalize();
    if (!Files.exists(this.rootPath)) {
      try {
        Files.createDirectories(this.rootPath);
      } catch (IOException e) {
        logger.warn("本地文件存储器无法创建目录：{}", root);
        return false;
      }
    }

    String granularity = config.get("granularity").asText();
    if (Strings.isBlank(granularity)) {
      logger.warn("本地文件存储器未配置存储颗粒度[granularity]，将使用默认值：{}", StorageGranularity.NONE);
      granularity = StorageGranularity.NONE.toString();
    }
    try {
      this.granularity = StorageGranularity.valueOf(granularity);
    } catch (Exception e) {
      logger.error("本地文件存储器配置了不存在的颗粒度 [{}]，将使用默认值：{}", granularity, StorageGranularity.NONE);
      this.granularity = StorageGranularity.NONE;
    }

    return true;
  }

  @Override
  public void destroy() {
    // do nothing
  }

  @Override
  public Path store(InputStream inputStream, String filename) {
    String path = granularity.resolveKeyPrefix();
    Path targetDir = rootPath.resolve(path);
    Path targetFile = targetDir.resolve(filename).normalize();

    // 安全检查：确保目标文件在根路径下
    if (!targetFile.startsWith(rootPath)) {
      throw new IllegalArgumentException("文件存储路径超出根路径范围");
    }

    try {
      Files.createDirectories(targetDir);
      try (var os = Files.newOutputStream(targetFile)) {
        FileCopyUtils.copy(inputStream, os);
      }
    } catch (IOException e) {
      throw new RuntimeException("文件存储失败: " + targetFile, e);
    }

    return targetFile;
  }
}
