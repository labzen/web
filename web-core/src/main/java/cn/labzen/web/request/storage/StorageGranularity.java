package cn.labzen.web.request.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件存储目录组织粒度，供各类存储器共用。
 * <p>
 * 控制存储路径（或对象键前缀）的目录层级：
 * <ul>
 *   <li>{@code NONE} - 无子目录，所有文件平铺</li>
 *   <li>{@code YMD} - 一级目录：年-月-日，如 {@code 2026-05-14/}</li>
 *   <li>{@code YM_D} - 二级目录：年-月/日，如 {@code 2026-05/14/}</li>
 *   <li>{@code Y_M} - 二级目录：年/月，如 {@code 2026/05/}</li>
 *   <li>{@code Y_M_D} - 三级目录：年/月/日，如 {@code 2026/05/14/}</li>
 * </ul>
 */
public enum StorageGranularity {

  NONE,
  YMD,
  YM_D,
  Y_M,
  Y_M_D;

  private static final DateTimeFormatter YMD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  /**
   * 根据粒度解析对象键前缀（以 / 结尾），NONE 时返回空字符串
   *
   * @return 键前缀，如 "2026-05-14/" 或 "2026/05/14/"
   */
  public String resolveKeyPrefix() {
    if (this == NONE) {
      return "";
    }

    LocalDate now = LocalDate.now();
    return switch (this) {
      case YMD -> now.format(YMD_FORMATTER) + "/";
      case YM_D -> now.format(YM_FORMATTER) + "/" + String.format("%02d", now.getDayOfMonth()) + "/";
      case Y_M -> now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/";
      case Y_M_D -> now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" +
        String.format("%02d", now.getDayOfMonth()) + "/";
      default -> "";
    };
  }
}
