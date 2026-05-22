package cn.labzen.web.api.request;

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
   * 对需要存储的文件名重命名
   * <p/>
   * 可以不重命名，存储时，使用原文件名
   *
   * @param name 新文件名
   */
  void rename(String name);

  /**
   * 将文件存储到配置好的{@link FileStorage}中
   *
   * @return 文件存储后的完整访问地址
   */
  String store();

  /**
   * 将文件存储到指定存储器中
   *
   * @param storageName 存储器名称
   * @return 文件存储后的完整访问地址
   */
  String storeByStorage(String storageName);
//
//  /**
//   * 将文件存储到指定路径
//   *
//   * @param path 目标路径（字符串形式）
//   */
//  @Deprecated
//  void store(String path);
//
//  /**
//   * 将文件存储到指定路径
//   *
//   * @param path 目标路径
//   */
//  @Deprecated
//  void store(Path path);
}
