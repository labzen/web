package cn.labzen.web.log.bean;

import org.jspecify.annotations.NonNull;

/**
 * 上传文件详情
 *
 * @param filename    上传文件名称
 * @param size        上传文件大小（字节）
 * @param contentType 上传文件内容类型
 */
public record MultipartDetail(String filename, long size, String contentType) {

  @Override
  public @NonNull String toString() {
    return String.format("[%s (%sbytes, %s)]", filename, size, contentType);
  }
}
