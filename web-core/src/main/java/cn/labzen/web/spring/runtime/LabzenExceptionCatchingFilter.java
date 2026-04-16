package cn.labzen.web.spring.runtime;

import cn.labzen.logger.Loggers;
import cn.labzen.meta.exception.LabzenException;
import cn.labzen.meta.exception.LabzenRuntimeException;
import cn.labzen.spring.Springs;
import cn.labzen.tool.definition.Constants;
import cn.labzen.web.api.response.out.Response;
import cn.labzen.web.exception.RequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.Nonnull;
import java.io.IOException;

import static cn.labzen.web.api.definition.Constants.EXCEPTION_WAS_LOGGED_DURING_REQUEST;

/**
 * 异常捕捉过滤器
 * <p>
 * 作为最后一道防线，捕获经过 {@link HandlerExceptionResolver} 处理后仍未被捕获的异常，
 * 防止异常信息直接暴露给前端客户端。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>捕获 FilterChain 执行过程中抛出的所有异常</li>
 *   <li>根据异常类型构建标准化的响应结构</li>
 *   <li>处理日志记录，避免重复打印</li>
 * </ul>
 */
public class LabzenExceptionCatchingFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper = Springs.bean(ObjectMapper.class).orElseGet(ObjectMapper::new);

  /**
   * 过滤器的核心逻辑
   * <p>
   * 执行 FilterChain，如果发生异常则进入异常处理分支。
   */
  @Override
  protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                  @Nonnull HttpServletResponse response,
                                  @Nonnull FilterChain filterChain) {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      logging(request, e);
      output(request, response, e);
    }
  }

  /**
   * 记录异常日志
   * <p>
   * 仅在未被记录过的情况下记录日志，避免 HandlerExceptionResolver 已处理的异常被重复打印。
   */
  private void logging(HttpServletRequest request, Exception e) {
    Object attribute = request.getAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST);
    if (attribute == null) {
      if ((e instanceof RequestException re && re.isLogging()) || !(e instanceof RequestException)) {
        var logger = Loggers.getLogger(e.getStackTrace()[0].getClassName());
        logger.error("Exception caught in filter", e);
        request.setAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST, true);
      }
    }
  }

  /**
   * 根据异常类型输出标准化响应
   * <p>
   * 异常分类处理：
   * <ul>
   *   <li>RequestException - 使用其自定义的错误码和消息</li>
   *   <li>LabzenRuntimeException/LabzenException - 返回500内部错误</li>
   *   <li>其他异常 - 递归查找根因后返回</li>
   * </ul>
   */
  private void output(HttpServletRequest request, HttpServletResponse response, Exception e) {
    if (e instanceof RequestException re) {
      var data = new Response(re.getCode(), re.getMessage() != null ? re.getMessage() : HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null);
      sendMessage(data, request, response);
    } else if (e instanceof LabzenRuntimeException || e instanceof LabzenException) {
      var resp = new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        e.getMessage() != null ? e.getMessage() : HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null, null);
      sendMessage(resp, request, response);
    } else {
      Throwable throwable = findRootCause(e);
      var resp = new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        throwable.getMessage() != null ? throwable.getMessage() : HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null, null);
      sendMessage(resp, request, response);
    }
  }

  /**
   * 递归查找异常的根因
   */
  private Throwable findRootCause(Throwable exception) {
    if (exception.getCause() == null || exception == exception.getCause()) {
      return exception;
    }
    return findRootCause(exception.getCause());
  }

  /**
   * 发送 JSON 响应
   * <p>
   * 使用客户端请求的 Accept Header 作为 Content-Type，默认为 application/json。
   */
  private void sendMessage(Object message, HttpServletRequest request, HttpServletResponse response) {
    String contentType = request.getHeader("Accept");
    if (contentType == null) {
      contentType = MediaType.APPLICATION_JSON_VALUE;
    }
    response.setContentType(contentType);
    response.setStatus(HttpStatus.OK.value());
    response.setCharacterEncoding(Constants.DEFAULT_CHARSET_NAME);
    try {
      objectMapper.writeValue(response.getWriter(), message);
      response.getWriter().flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
