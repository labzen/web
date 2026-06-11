package cn.labzen.web.request;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.request.FileStorage;
import cn.labzen.web.api.request.UploadedFile;
import cn.labzen.web.exception.FileUploadException;
import cn.labzen.web.meta.WebCoreConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件存储管理器
 * <p>
 * 通过 SPI 加载 {@link FileStorage} 实现，管理存储器实例的生命周期，
 * 并提供统一的上传文件存储入口。
 * <p>
 * 使用方式：
 * <pre>
 *   &#064;Resource
 *   private FileStorageManager fileStorageManager;
 *
 *   // 使用默认存储器
 *   Path path = fileStorageManager.store(uploadedFile);
 *
 *   // 使用指定存储器
 *   Path path = fileStorageManager.store("QiniuFileStorage", uploadedFile);
 * </pre>
 */
@Component
@Slf4j
public final class FileStorageManager implements SmartInitializingSingleton, DisposableBean {

  private static final String DEFAULT_FILE_STORAGE_NAME = "LocalFileStorage";
  private static final int INTERNAL_SERVER_ERROR_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final Map<String, FileStorage> fileStorageMap = Maps.newConcurrentMap();

  @Resource
  private ObjectMapper mapper;
  private FileStorage defaultFileStorage;

  @Override
  public void afterSingletonsInstantiated() {
    ServiceLoader<FileStorage> loader = ServiceLoader.load(FileStorage.class);
    for (FileStorage storage : loader) {
      String name = storage.getClass().getSimpleName();
      fileStorageMap.put(name, storage);
    }
  }

  @Override
  public void destroy() {
    fileStorageMap.values().forEach(FileStorage::destroy);
  }

  public void initialize(Map<String, String> configurations) {
    boolean notInitialize = initialized.compareAndSet(false, true);
    if (!notInitialize) {
      logger.warn("存储器管理器已被初始化过，重复初始化可能会造成不可预料的问题，请检查是否确实有必要重新初始化！");
    }

    configurations.forEach((name, config) -> {
      FileStorage fileStorage = fileStorageMap.get(name);
      if (fileStorage == null) {
        logger.warn("无法找到配置名为 {} 的存储器，该配置将被忽略", name);
      } else {
        boolean configured = configure(name, fileStorage, config);
        if (!configured) {
          fileStorageMap.remove(name);
        }
      }
    });

    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    String defaultedFileStorageName = configuration.defaultFileStorage();
    FileStorage fileStorage = fileStorageMap.get(defaultedFileStorageName);
    if (fileStorage == null) {
      logger.warn("无法找到名为 {} 的可用存储器实例，将使用默认存储器", defaultedFileStorageName);
      fileStorage = fileStorageMap.get(DEFAULT_FILE_STORAGE_NAME);
      if (fileStorage == null) {
        logger.error("最终无法找到任何可用的默认存储器实例，在执行存储操作时可能会导致异常，默认存储器名：{}", DEFAULT_FILE_STORAGE_NAME);
      }
    }

    defaultFileStorage = fileStorage;
    logger.info("存储器初始化过程已完成...");
  }

  private boolean configure(String name, FileStorage fileStorage, String config) {
    try {
      JsonNode jsonNode = mapper.readTree(config);
      return fileStorage.initialize(jsonNode);
    } catch (Exception e) {
      logger.atError().setCause(e).log("存储器 [{}] 在进行初始化时失败，该存储器实例不可用", name);
      return false;
    }
  }

  // ===================================================================================================================
  // 文件存储方法
  // ===================================================================================================================

  /**
   * 使用默认存储器存储上传文件
   *
   * @param file 已上传的文件
   * @return 存储路径
   */
  public Path store(UploadedFile file) {
    FileStorage storage = getDefault();
    try {
      return storage.store(file);
    } catch (IOException e) {
      throw new FileUploadException(INTERNAL_SERVER_ERROR_CODE, e, "文件存储失败");
    }
  }

  /**
   * 使用指定存储器存储上传文件
   *
   * @param storageName 存储器名称
   * @param file        已上传的文件
   * @return 存储路径
   */
  public Path store(String storageName, UploadedFile file) {
    FileStorage storage = get(storageName);
    try {
      return storage.store(file);
    } catch (IOException e) {
      throw new FileUploadException(INTERNAL_SERVER_ERROR_CODE, e, "文件存储失败");
    }
  }

  // ===================================================================================================================
  // 存储器访问方法
  // ===================================================================================================================

  /**
   * 获取默认存储器
   */
  public FileStorage getDefault() {
    if (defaultFileStorage == null) {
      throw new IllegalStateException("FileStorageManager 尚未初始化或无可用存储实例");
    }
    return defaultFileStorage;
  }

  /**
   * 根据名称获取存储器
   */
  public FileStorage get(String name) {
    FileStorage storage = fileStorageMap.get(name);
    if (storage == null) {
      throw new IllegalArgumentException("未找到名为 '" + name + "' 的存储器实例");
    }
    return storage;
  }
}
