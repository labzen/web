package cn.labzen.web.api.request;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储接口。
 * <p>
 * 定义将文件内容存储到目标位置的抽象。支持直接存储 {@link UploadedFile} 实例。
 */
public interface FileStorage {

  /**
   * 初始化文件存储器
   *
   * @param config 配置参数，存储器实现可自行解析
   */
  boolean initialize(JsonNode config);

  /**
   * 销毁文件存储器实例
   */
  void destroy();

  /**
   * 将输入流存储到以指定文件名命名的目标位置，并返回实际存储路径
   *
   * @param inputStream 文件输入流
   * @param filename    文件名，用于确定存储文件名
   * @return 实际存储的完整路径
   */
  Path store(InputStream inputStream, String filename);

  /**
   * 将 {@link UploadedFile} 实例存储到目标位置
   * <p>
   * 默认实现从 UploadedFile 中提取输入流和文件名，调用 {@link #store(InputStream, String)}。
   * 子类可覆盖以直接使用 UploadedFile 的元数据（如 contentType、size）进行优化。
   *
   * @param file 已上传的文件实例
   * @return 实际存储的路径
   * @throws IOException 读取或存储失败时抛出
   */
  default Path store(UploadedFile file) throws IOException {
    return store(file.getInputStream(), file.originalFilename());
  }
}
