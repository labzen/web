package cn.labzen.web.request;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.request.UploadedFile;
import cn.labzen.web.exception.FileUploadException;
import cn.labzen.web.meta.WebCoreConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public class StandardUploadedFile implements UploadedFile {

  private static final int NOT_ACCEPTABLE_CODE = HttpStatus.NOT_ACCEPTABLE.value();
  private static final int INTERNAL_SERVER_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
  private static final List<String> acceptedUploadFileExtensions;
  private static volatile cn.labzen.web.api.request.FileStorage fileStorage;

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    acceptedUploadFileExtensions = configuration.acceptedUploadFileExtensions();
  }

  private final MultipartFile multipartFile;
  private String extension;
  private String storageFileName;

  public StandardUploadedFile(MultipartFile multipartFile) {
    this.multipartFile = multipartFile;
    check();
  }

  private void check() {
    if (multipartFile == null || multipartFile.isEmpty()) {
      throw new FileUploadException(NOT_ACCEPTABLE_CODE, "文件不能为空");
    }
    String originalFilename = multipartFile.getOriginalFilename();
    if (originalFilename == null || originalFilename.trim().isEmpty()) {
      throw new FileUploadException(NOT_ACCEPTABLE_CODE, "文件名不能为空");
    }
    if (Strings.containsAny(originalFilename, "..", "/", "\\")) {
      throw new FileUploadException(NOT_ACCEPTABLE_CODE, "文件名包含非法字符");
    }
    String extension = Strings.lastUntil(originalFilename, ".", false);
    if (originalFilename.equals(extension)) {
      extension = "";
    }
    if (extension.isEmpty()) {
      throw new FileUploadException(NOT_ACCEPTABLE_CODE, "文件缺少扩展名");
    }
    if (!acceptedUploadFileExtensions.contains(extension)) {
      throw new FileUploadException(NOT_ACCEPTABLE_CODE, "不支持的文件类型: {}", extension);
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
  public void rename(String name) {
    this.storageFileName = name;
  }

  @Override
  public String store() {
    try {
      Path path = FileStorageManager.get().store(multipartFile.getInputStream(), Strings.value(storageFileName, originalFilename()));
      return path.toAbsolutePath().toString();
    } catch (Exception e) {
      throw new FileUploadException(INTERNAL_SERVER_CODE, e, "文件存储失败");
    }
  }

  public String storeByStorage(String storageName) {
    try {
      Path path = FileStorageManager.get(storageName).store(multipartFile.getInputStream(), Strings.value(storageFileName, originalFilename()));
      return path.toAbsolutePath().toString();
    } catch (Exception e) {
      throw new FileUploadException(INTERNAL_SERVER_CODE, e, "文件存储失败");
    }
  }
}
