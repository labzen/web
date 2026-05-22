package cn.labzen.web.request.storage;

import cn.labzen.tool.util.IOs;
import cn.labzen.web.api.request.FileStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.obs.services.ObsClient;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 华为云 OBS 对象存储实现。
 * <p>
 * 初始化参数为 JSON 格式，示例：
 * <pre>
 * {
 *   "endPoint": "obs.cn-north-4.myhuaweicloud.com",
 *   "ak": "...",
 *   "sk": "...",
 *   "bucketName": "my-bucket",
 *   "granularity": "YMD"
 * }
 * </pre>
 *
 * @see StorageGranularity
 */
@Slf4j
public class HuaweiObsFileStorage implements FileStorage {

  private String endPoint;
  private String ak;
  private String sk;
  private String bucketName;
  private StorageGranularity granularity;
  private ObsClient obsClient;

  @Override
  public boolean initialize(JsonNode config) {
    this.endPoint = config.get("endPoint").asText();
    this.ak = config.get("ak").asText();
    this.sk = config.get("sk").asText();
    this.bucketName = config.get("bucketName").asText();

    String gran = config.has("granularity") ? config.get("granularity").asText() : "NONE";
    try {
      this.granularity = StorageGranularity.valueOf(gran);
    } catch (IllegalArgumentException e) {
      logger.warn("华为云OBS存储器配置了不支持的颗粒度 [{}]，使用默认值 NONE", gran);
      this.granularity = StorageGranularity.NONE;
      return false;
    }

    this.obsClient = new ObsClient(ak, sk, endPoint);
    return true;
  }

  @Override
  public void destroy() {
    if (obsClient != null) {
      IOs.closeQuietly(obsClient);
    }
  }

  @Override
  public Path store(InputStream inputStream, String filename) {
    String key = granularity.resolveKeyPrefix() + filename;
    try {
      obsClient.putObject(bucketName, key, inputStream);
    } catch (Exception e) {
      throw new RuntimeException("华为云OBS文件上传失败: " + key, e);
    }
    return Path.of(key);
  }
}
