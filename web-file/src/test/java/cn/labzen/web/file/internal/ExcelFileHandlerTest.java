package cn.labzen.web.file.internal;

import cn.labzen.web.file.internal.excel.ExcelFileHandler;
import cn.labzen.web.file.test.DataBean;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelFileHandlerTest {

  private Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    tempDir = Files.createTempDirectory("excel-test");
    System.out.println(tempDir.toAbsolutePath());
  }

  @Test
  void testHandle_withValidData_shouldGenerateExcelFile() throws IOException {
    // Given
    List<DataBean> data = createTestData();
    File outputFile = tempDir.resolve("test-output.xlsx").toFile();

    ExcelFileHandler<DataBean> handler = new ExcelFileHandler<>(DataBean.class);
    handler.setData(data);
    handler.setOutputFilePath(outputFile.getAbsolutePath());

    // When
    handler.handle();

    // Then
    assertTrue(outputFile.exists(), "Excel 文件应该被创建");
    assertTrue(outputFile.length() > 0, "Excel 文件不应该为空");
    assertEquals("xlsx", getFileExtension(outputFile.getName()), "文件扩展名应该是 xlsx");
  }

  @Test
  void testHandle_withEmptyData_shouldGenerateExcelFileWithHeaders() throws IOException {
    // Given
    List<DataBean> emptyData = Lists.newArrayList();
    File outputFile = tempDir.resolve("test-empty.xlsx").toFile();

    ExcelFileHandler<DataBean> handler = new ExcelFileHandler<>(DataBean.class);
    handler.setData(emptyData);
    handler.setOutputFilePath(outputFile.getAbsolutePath());

    // When
    handler.handle();

    // Then
    assertTrue(outputFile.exists(), "即使没有数据，Excel 文件也应该被创建（包含表头）");
    assertTrue(outputFile.length() > 0, "Excel 文件不应该为空（至少有表头）");
  }

  @Test
  void testHandle_withNullOutputPath_shouldThrowException() {
    // Given
    List<DataBean> data = createTestData();

    ExcelFileHandler<DataBean> handler = new ExcelFileHandler<>(DataBean.class);
    handler.setData(data);
    // 不设置输出路径

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, handler::handle);
    assertEquals("未指定输出文件", exception.getMessage());
  }

  @Test
  void testHandle_withMultipleRecords_shouldGenerateCompleteExcel() throws IOException {
    // Given
    List<DataBean> data = createTestDataWithMultipleRecords(50);
    File outputFile = tempDir.resolve("test-multiple.xlsx").toFile();

    ExcelFileHandler<DataBean> handler = new ExcelFileHandler<>(DataBean.class);
    handler.setData(data);
    handler.setOutputFilePath(outputFile.getAbsolutePath());

    // When
    handler.handle();

    // Then
    assertTrue(outputFile.exists(), "Excel 文件应该被创建");
    assertTrue(outputFile.length() > 0, "Excel 文件不应该为空");
  }

  private List<DataBean> createTestData() {
    List<DataBean> data = Lists.newArrayList();
    for (int i = 0; i < 20; i++) {
      DataBean bean = new DataBean();
      bean.setId((long) i);
      bean.setName("张三" + i);
      bean.setAge(18 + i);
      bean.setGraduated(true);
      bean.setBirthDate(new Date());
      bean.setScore(90.5 + i);
      bean.setHeight(1.8F + i);
      bean.setCreateTime(LocalDateTime.now());

      data.add(bean);
    }
    return data;
  }

  private List<DataBean> createTestDataWithMultipleRecords(int count) {
    List<DataBean> data = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      DataBean bean = new DataBean();
      bean.setId((long) i);
      bean.setName("用户" + i);
      bean.setAge(20 + i);
      bean.setGraduated(i % 2 == 0);
      bean.setBirthDate(new Date(System.currentTimeMillis() - i * 86400000));
      bean.setScore(80.0 + i);
      bean.setHeight(1.7F + (i * 0.01F));
      bean.setCreateTime(LocalDateTime.now().minusHours(i));

      data.add(bean);
    }
    return data;
  }

  private String getFileExtension(String fileName) {
    int lastDotIndex = fileName.lastIndexOf(".");
    if (lastDotIndex == -1) {
      return "";
    }
    return fileName.substring(lastDotIndex + 1).toLowerCase();
  }
}
