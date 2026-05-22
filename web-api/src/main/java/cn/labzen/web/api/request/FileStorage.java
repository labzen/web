package cn.labzen.web.api.request;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储接口。
 * <p>
 * 定义将文件内容（输入流）存储到目标位置的抽象。
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
   * 将输入流存储到以原始文件名命名的目标位置，并返回实际存储路径
   *
   * @param inputStream 文件输入流
   * @param filename    文件名，用于确定存储文件名
   * @return 实际存储的完整路径
   */
  Path store(InputStream inputStream, String filename);
}
