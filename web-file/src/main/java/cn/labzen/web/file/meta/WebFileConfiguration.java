package cn.labzen.web.file.meta;

import cn.labzen.meta.configuration.annotation.Configured;
import cn.labzen.meta.configuration.annotation.Item;

@Configured(namespace = "web.file")
public interface WebFileConfiguration {

  /**
   * 最大上传文件大小，默认10MB
   */
  @Item(path = "max-file-size", required = false, defaultValue = "10485760")
  // 10MB in bytes
  long maxFileSize();

  /**
   * 临时文件过期时间（毫秒），仅对内存存储有效
   */
  @Item(path = "temp-file-expiration", required = false, defaultValue = "300000")
  // 5分钟
  long tempFileExpiration();
}
