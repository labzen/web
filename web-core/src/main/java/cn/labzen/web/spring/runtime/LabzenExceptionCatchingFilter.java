package cn.labzen.web.spring.runtime;

import cn.labzen.logger.Loggers;
import cn.labzen.meta.exception.LabzenException;
import cn.labzen.meta.exception.LabzenRuntimeException;
import cn.labzen.spring.Springs;
import cn.labzen.tool.definition.Constants;
import cn.labzen.web.exception.RequestException;
import cn.labzen.web.api.response.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.Nonnull;
import java.io.IOException;

import static cn.labzen.web.api.definition.Constants.EXCEPTION_WAS_LOGGED_DURING_REQUEST;

/**
 * 异常捕捉过滤器，防止有异常没有经过 {@link HandlerExceptionResolver} 处理，而被直接抛出到前端
 */
public class LabzenExceptionCatchingFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper = Springs.bean(ObjectMapper.class).orElseGet(ObjectMapper::new);

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

  private Throwable findRootCause(Throwable exception) {
    if (exception.getCause() == null || exception == exception.getCause()) {
      return exception;
    }
    return findRootCause(exception.getCause());
  }

  private void sendMessage(Object message, HttpServletRequest request, HttpServletResponse response) {
    String contentType = request.getHeader("Accept");
    response.setStatus(HttpStatus.OK.value());
    response.setContentType(contentType);
    response.setCharacterEncoding(Constants.DEFAULT_CHARSET_NAME);
    try {
      objectMapper.writeValue(response.getWriter(), message);
      response.getWriter().flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
