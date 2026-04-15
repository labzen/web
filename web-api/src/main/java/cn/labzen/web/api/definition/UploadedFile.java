package cn.labzen.web.api.definition;

import java.nio.file.Path;

/**
 * 上传文件接口。
 * <p>
 * 定义了已上传文件的基本信息及存储操作。
 */
public interface UploadedFile {

  /**
   * 获取文件 MIME 类型
   *
   * @return 文件内容类型
   */
  String contentType();

  /**
   * 获取文件大小（字节）
   *
   * @return 文件大小
   */
  long size();

  /**
   * 获取原始文件名
   *
   * @return 原始文件名
   */
  String originalFilename();

  /**
   * 获取文件扩展名
   *
   * @return 文件扩展名（不含点号）
   */
  String extension();

  /**
   * 将文件存储到指定路径
   *
   * @param path 目标路径（字符串形式）
   */
  void store(String path);

  /**
   * 将文件存储到指定路径
   *
   * @param path 目标路径
   */
  void store(Path path);
}
