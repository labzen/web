package cn.labzen.web.spring.runtime;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取请求体的请求包装器。
 * <p>
 * Servlet 请求体是一次性流，读取一次即被消费。本包装器在通过 {@link #getReader()} 首次读取时，
 * 将请求体缓存为字节数组；此后 {@link #getReader()} 与 {@link #getInputStream()} 均从缓存提供内容，
 * 使请求体可被多次读取（例如 API 日志与 {@code @RequestBody} 各读一次）。
 * <p>
 * 缓存按需触发：仅当调用 {@link #getReader()} 时才读取并缓存。未调用时 {@link #getInputStream()}
 * 直接透传原始请求，不产生额外的缓冲开销。{@code multipart/form-data} 请求不做缓存，全部直接透传。
 */
public class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

  private byte[] cachedBody;

  public RepeatableReadRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean cacheable() {
    String contentType = getContentType();
    return contentType == null || !contentType.contains("multipart/form-data");
  }

  @Override
  public BufferedReader getReader() throws IOException {
    if (!cacheable()) {
      return super.getReader();
    }
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody()), resolveCharset()));
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (!cacheable() || cachedBody == null) {
      return super.getInputStream();
    }
    return servletInputStream(new ByteArrayInputStream(cachedBody));
  }

  private byte[] cachedBody() throws IOException {
    if (cachedBody == null) {
      try (InputStream in = super.getInputStream()) {
        cachedBody = in.readAllBytes();
      }
    }
    return cachedBody;
  }

  /**
   * 解析读取请求体所使用的字符集。
   * <p>
   * 请求未声明字符集或声明为 ISO-8859-1 时使用 UTF-8，避免 JSON 中的非 ASCII 内容出现乱码。
   */
  private Charset resolveCharset() {
    String encoding = getCharacterEncoding();
    if (encoding != null && !encoding.isBlank() && !StandardCharsets.ISO_8859_1.name().equalsIgnoreCase(encoding)) {
      return Charset.forName(encoding);
    }
    return StandardCharsets.UTF_8;
  }

  private static ServletInputStream servletInputStream(ByteArrayInputStream source) {
    return new ServletInputStream() {

      @Override
      public int read() {
        return source.read();
      }

      @Override
      public int read(@Nonnull byte[] b, int off, int len) {
        return source.read(b, off, len);
      }

      @Override
      public boolean isFinished() {
        return source.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("暂不支持异步读取请求体");
      }
    };
  }
}
