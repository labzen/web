package cn.labzen.web.request;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.request.FileStorage;
import cn.labzen.web.meta.WebCoreConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public final class FileStorageManager implements SmartInitializingSingleton, DisposableBean {

  private static final String DEFAULT_FILE_STORAGE_NAME = "LocalFileStorage";

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final Map<String, FileStorage> fileStorageMap = Maps.newConcurrentMap();

  @Resource
  private ObjectMapper mapper;
  private FileStorage defaultFileStorage;

  @Override
  public void afterSingletonsInstantiated() {
    Holder.INSTANCE = this;

    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    List<String> fileExtensions = configuration.acceptedUploadFileExtensions();
    StandardUploadedFile.setAcceptedUploadFileExtensions(fileExtensions);

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

  private static class Holder {
    private static volatile FileStorageManager INSTANCE;
  }

  public void initialize(@Nonnull Map<String, String> configurations) {
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

  static FileStorage get() {
    FileStorage storage = Holder.INSTANCE.defaultFileStorage;
    if (storage == null) {
      throw new IllegalStateException("FileStorageManager 尚未初始化或无可用存储实例");
    }
    return storage;
  }

  static FileStorage get(String name) {
    FileStorage storage = Holder.INSTANCE.fileStorageMap.get(name);
    if (storage == null) {
      throw new IllegalArgumentException("未找到名为 '" + name + "' 的存储器实例");
    }
    return storage;
  }
}
