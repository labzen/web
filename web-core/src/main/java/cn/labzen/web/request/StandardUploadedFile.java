package cn.labzen.web.request;

import cn.labzen.meta.Labzens;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.request.UploadedFile;
import cn.labzen.web.exception.FileUploadException;
import cn.labzen.web.meta.WebCoreConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 标准上传文件实现
 * <p>
 * 封装 Spring 的 {@link MultipartFile}，提供文件校验和基本信息访问。
 * 文件存储操作由 {@link FileStorageManager} 负责，不再由本类发起。
 */
public class StandardUploadedFile implements UploadedFile {

  private static final int NOT_ACCEPTABLE_CODE = HttpStatus.NOT_ACCEPTABLE.value();

  private static final List<String> ACCEPTED_UPLOAD_FILE_EXTENSIONS;

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    List<String> fileExtensions = configuration.acceptedUploadFileExtensions();
    ACCEPTED_UPLOAD_FILE_EXTENSIONS = List.copyOf(fileExtensions);
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
    if (!ACCEPTED_UPLOAD_FILE_EXTENSIONS.contains(extension)) {
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
  public InputStream getInputStream() throws IOException {
    return multipartFile.getInputStream();
  }
}
