package cn.labzen.web.request.storage;

import cn.labzen.web.api.request.FileStorage;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 阿里云 OSS 对象存储实现。
 * <p>
 * 初始化参数为 JSON 格式，示例：
 * <pre>
 * {
 *   "endpoint": "oss-cn-hangzhou.aliyuncs.com",
 *   "accessKeyId": "...",
 *   "accessKeySecret": "...",
 *   "bucket": "my-bucket",
 *   "granularity": "YMD"
 * }
 * </pre>
 *
 * @see StorageGranularity
 */
@Slf4j
public class AliyunOssFileStorage implements FileStorage {

  private String endpoint;
  private String accessKeyId;
  private String accessKeySecret;
  private String bucket;
  private StorageGranularity granularity;
  private OSS ossClient;

  @Override
  public boolean initialize(JsonNode config) {
    this.endpoint = config.get("endpoint").asText();
    this.accessKeyId = config.get("accessKeyId").asText();
    this.accessKeySecret = config.get("accessKeySecret").asText();
    this.bucket = config.get("bucket").asText();

    String gran = config.has("granularity") ? config.get("granularity").asText() : "NONE";
    try {
      this.granularity = StorageGranularity.valueOf(gran);
    } catch (IllegalArgumentException e) {
      logger.warn("阿里云OSS存储器配置了不支持的颗粒度 [{}]，使用默认值 NONE", gran);
      this.granularity = StorageGranularity.NONE;
    }

    this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    return true;
  }

  @Override
  public Path store(InputStream inputStream, String filename) {
    String key = granularity.resolveKeyPrefix() + filename;
    try {
      ossClient.putObject(bucket, key, inputStream);
    } catch (Exception e) {
      throw new RuntimeException("阿里云OSS文件上传失败: " + key, e);
    }
    return Path.of(key);
  }
}
