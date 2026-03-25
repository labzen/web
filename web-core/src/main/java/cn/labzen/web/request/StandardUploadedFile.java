package cn.labzen.web.request;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.definition.UploadedFile;
import cn.labzen.web.exception.WebFileException;
import cn.labzen.web.meta.WebCoreConfiguration;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class StandardUploadedFile implements UploadedFile {

  private static final List<String> acceptedUploadFileExtensions;

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    acceptedUploadFileExtensions = configuration.acceptedUploadFileExtensions();
  }

  private final MultipartFile multipartFile;
  private String extension;

  public StandardUploadedFile(MultipartFile multipartFile) {
    this.multipartFile = multipartFile;
    check();
  }

  private void check() {
    if (multipartFile == null || multipartFile.isEmpty()) {
      throw new WebFileException(500, "文件不能为空");
    }
    String originalFilename = multipartFile.getOriginalFilename();
    if (originalFilename == null || originalFilename.trim().isEmpty()) {
      throw new IllegalArgumentException("文件名不能为空");
    }
    if (Strings.containsAny(originalFilename, "..", "/", "\\")) {
      throw new IllegalArgumentException("文件名包含非法字符");
    }
    String extension = Strings.lastUntil(originalFilename, ".", false);
    if (originalFilename.equals(extension)) {
      extension = "";
    }
    if (extension.isEmpty()) {
      throw new IllegalArgumentException("文件缺少扩展名");
    }
    if (!acceptedUploadFileExtensions.contains(extension)) {
      throw new IllegalArgumentException("不支持的文件类型: " + extension);
    }
    this.extension = extension;
  }

  @Override
  public String contentType() {
    return multipartFile.getContentType();
  }

  @Override
  public long size() {
    return multipartFile.getSize();
  }

  @Override
  public String originalFilename() {
    return multipartFile.getOriginalFilename();
  }

  @Override
  public String extension() {
    return extension;
  }

  @Override
  public void store(String path) {
    store(Paths.get(path));
  }

  @Override
  public void store(Path path) {
    try (InputStream is = multipartFile.getInputStream()) {
      FileCopyUtils.copy(is, Files.newOutputStream(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
