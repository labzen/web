package cn.labzen.web.request.storage;

import cn.labzen.web.api.request.FileStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 七牛云对象存储实现。
 * <p>
 * 初始化参数为 JSON 格式，示例：
 * <pre>
 * {
 *   "accessKey": "...",
 *   "secretKey": "...",
 *   "bucket": "my-bucket",
 *   "domain": "https://cdn.example.com",
 *   "granularity": "YMD"
 * }
 * </pre>
 *
 * @see StorageGranularity
 */
@Slf4j
public class QiniuFileStorage implements FileStorage {

  private String accessKey;
  private String secretKey;
  private String bucket;
//  private String domain;
  private StorageGranularity granularity;
  private UploadManager uploadManager;

  @Override
  public boolean initialize(JsonNode config) {
    this.accessKey = config.get("accessKey").asText();
    this.secretKey = config.get("secretKey").asText();
    this.bucket = config.get("bucket").asText();
//    this.domain = config.get("domain").asText();

    String gran = config.has("granularity") ? config.get("granularity").asText() : "NONE";
    try {
      this.granularity = StorageGranularity.valueOf(gran);
    } catch (IllegalArgumentException e) {
      logger.warn("七牛云存储器配置了不支持的颗粒度 [{}]，使用默认值 NONE", gran);
      this.granularity = StorageGranularity.NONE;
    }

    this.uploadManager = new UploadManager(Configuration.create());
    return true;
  }

  @Override
  public void destroy() {
    // do nothing
  }

  @Override
  public Path store(InputStream inputStream, String filename) {
    String key = granularity.resolveKeyPrefix() + filename;
    Auth auth = Auth.create(accessKey, secretKey);
    String upToken = auth.uploadToken(bucket);

    try {
      Response response = uploadManager.put(inputStream, key, upToken, null, null);
      if (!response.isOK()) {
        throw new RuntimeException("七牛云文件上传失败: " + response);
      }
    } catch (Exception e) {
      throw new RuntimeException("七牛云文件上传失败: " + key, e);
    }

    return Path.of(key);
  }
}
