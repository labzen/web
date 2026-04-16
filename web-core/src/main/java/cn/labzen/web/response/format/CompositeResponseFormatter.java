package cn.labzen.web.response.format;

import cn.labzen.web.api.response.out.Response;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.ServiceLoader;

/**
 * 响应格式化器组合
 * <p>
 * 负责协调多个响应格式化器，按优先级顺序处理 Controller 返回值。
 * <p>
 * 内置格式化器顺序：
 * <ol>
 *   <li>ResponseAgainResponseFormatter - 已经是 Response 结构则快速返回</li>
 *   <li>AbnormalStatusResponseFormatter - 处理异常的 HTTP 状态码（如404）</li>
 *   <li>... SPI 加载的格式化器 ...</li>
 *   <li>FileDownloadResponseFormatter - 处理文件下载响应</li>
 *   <li>StandardResultResponseFormatter - 处理标准的 Result 返回结构</li>
 *   <li>UnexpectedResponseFormatter - 处理未预期的返回值</li>
 * </ol>
 */
public class CompositeResponseFormatter implements ResponseFormatter {

  private final List<ResponseFormatter> formatters;

  /**
   * 构造方法
   * <p>
   * 初始化内置格式化器，并加载 SPI 扩展的格式化器。
   */
  public CompositeResponseFormatter() {
    // 第1 处理已经是 Response 结构化的情况，快速返回已经是 Response 结构的情况
    ResponseAgainResponseFormatter responseAgainRF = new ResponseAgainResponseFormatter();
    // 第2 格式化不正常的 Http Status 结果，如404
    AbnormalStatusResponseFormatter abnormalStatusRF = new AbnormalStatusResponseFormatter();

    // 倒3 处理文件下载的情况
    FileDownloadResponseFormatter fileDownloadRF = new FileDownloadResponseFormatter();
    // 倒2 处理 Result 中的返回值格式化，标准的返回结构
    StandardResultResponseFormatter standardResultRF = new StandardResultResponseFormatter();
    // 倒1 处理前面所有格式化器都未考虑到的请
    UnexpectedResponseFormatter unexpectedRF = new UnexpectedResponseFormatter();

    List<ResponseFormatter> allFormatters = Lists.newArrayList(responseAgainRF, abnormalStatusRF);
    ServiceLoader<ResponseFormatter> loadedFormatters = ServiceLoader.load(ResponseFormatter.class, this.getClass().getClassLoader());
    for (ResponseFormatter loadedFormatter : loadedFormatters) {
      allFormatters.add(loadedFormatter);
    }
    allFormatters.add(fileDownloadRF);
    allFormatters.add(standardResultRF);
    allFormatters.add(unexpectedRF);

    this.formatters = List.copyOf(allFormatters);
  }

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return true;
  }

  /**
   * 格式化响应
   * <p>
   * 遍历所有格式化器，找到第一个支持当前返回值类型的格式化器进行处理。
   * 如果结果为 null，返回 204 NO_CONTENT 响应。
   */
  @Override
  public Object format(Object result, HttpServletRequest request, HttpServletResponse response) {
    if (result == null) {
      return new Response(HttpStatus.NO_CONTENT.value(), HttpStatus.NO_CONTENT.getReasonPhrase(), null, null);
    }

    Class<?> targetClass = AopUtils.getTargetClass(result);
    for (ResponseFormatter formatter : formatters) {
      if (formatter.support(targetClass, request)) {
        return formatter.format(result, request, response);
      }
    }

    return new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "不阔能，绝对不阔能", null, null);
  }
}
