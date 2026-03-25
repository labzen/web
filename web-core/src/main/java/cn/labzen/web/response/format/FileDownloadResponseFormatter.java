package cn.labzen.web.response.format;

import cn.labzen.web.api.response.result.FileResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FileDownloadResponseFormatter implements ResponseFormatter {

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return FileResult.class.isAssignableFrom(clazz);
  }

  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    FileResult fileResult = (FileResult) result;
    downloadByFile(response, fileResult.filename(), fileResult.value());
    return null;
  }

  private void downloadByFile(HttpServletResponse response, String filename, File file) {
    setDownloadHeaders(response, filename, file.length());

    // 流式传输文件内容
    try (FileInputStream inputStream = new FileInputStream(file);
         BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
         OutputStream outputStream = response.getOutputStream()) {

      byte[] buffer = new byte[8192]; // 8KB缓冲区
      int bytesRead;
      while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
        outputStream.flush(); // 立即刷新，实现流式传输
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 设置下载响应头
   */
  private void setDownloadHeaders(HttpServletResponse response, String fileName, long contentLength) {
    try {
      // 处理中文文件名
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
        .replaceAll("\\+", "%20");

      response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
      response.setHeader("Content-Disposition",
        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

      if (contentLength > 0) {
        response.setContentLengthLong(contentLength);
      }

      // 禁用缓存
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      response.setHeader("Pragma", "no-cache");
      response.setDateHeader("Expires", 0);

    } catch (Exception e) {
      throw new RuntimeException("设置文件下载响应头失败", e);
    }
  }
}
