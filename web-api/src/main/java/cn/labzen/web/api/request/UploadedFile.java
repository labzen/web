package cn.labzen.web.api.request;

import java.io.IOException;
import java.io.InputStream;

/**
 * 上传文件接口。
 * <p>
 * 定义了已上传文件的基本信息及内容读取能力。
 * 文件的存储操作由 {@link FileStorage} 负责，而非由上传文件自身发起。
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
   * 对需要存储的文件名重命名
   * <p>
   * 可以不重命名，存储时使用原文件名
   *
   * @param name 新文件名
   */
  void rename(String name);

  /**
   * 获取文件内容的输入流
   *
   * @return 文件输入流
   * @throws IOException 读取失败时抛出
   */
  InputStream getInputStream() throws IOException;
}
