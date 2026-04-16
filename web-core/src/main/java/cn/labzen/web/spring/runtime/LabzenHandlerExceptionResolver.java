package cn.labzen.web.spring.runtime;

import cn.labzen.logger.Loggers;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.api.response.out.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.labzen.web.api.definition.Constants.EXCEPTION_WAS_LOGGED_DURING_REQUEST;

/**
 * Spring MVC 异常解析器
 * <p>
 * 处理 Spring MVC 请求处理过程中的各类异常，将它们转换为标准化的 JSON 响应。
 * <p>
 * 支持的异常类型：
 * <ul>
 *   <li>BindException - 参数绑定失败（400）</li>
 *   <li>NoHandlerFoundException - 无对应处理器（404）</li>
 *   <li>HttpRequestMethodNotSupportedException - 请求方法不支持（405）</li>
 *   <li>HttpMediaTypeNotSupportedException - 媒体类型不支持（415）</li>
 *   <li>MissingServletRequestParameterException - 缺少请求参数（400）</li>
 *   <li>TypeMismatchException - 类型不匹配（400）</li>
 *   <li>ConversionNotSupportedException - 转换不支持（500）</li>
 *   <li>MissingPathVariableException - 缺少路径变量（500）</li>
 * </ul>
 *
 * @see HandlerExceptionResolver
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LabzenHandlerExceptionResolver implements HandlerExceptionResolver {

  private static final Class<Response> RESPONSE_TYPE = Response.class;
  @Resource
  private List<HttpMessageConverter<Object>> converters;

  /**
   * 解析异常并生成响应
   * <p>
   * 使用 pattern matching 匹配异常类型，分发到对应的处理器。
   */
  @Override
  public ModelAndView resolveException(@Nonnull HttpServletRequest request,
                                       @Nonnull HttpServletResponse response,
                                       Object handler,
                                       @Nonnull Exception ex) {
    Object attribute = request.getAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST);
    if (attribute == null) {
      var logger = Loggers.getLogger(ex.getStackTrace()[0].getClassName());
      logger.error("Exception caught by resolver", ex);
      request.setAttribute(EXCEPTION_WAS_LOGGED_DURING_REQUEST, true);
    }

    return switch (ex) {
      case BindException be -> handleBindException(request, response, be);
      case NoHandlerFoundException ignored -> handleNoHandlerFoundException(request, response);
      case HttpRequestMethodNotSupportedException he -> handleRequestMethodNotSupportedException(request, response, he);
      case HttpMediaTypeNotSupportedException he -> handleMediaTypeNotSupportedException(request, response, he);
      case MissingPathVariableException he -> handleMissingPathVariableException(request, response, he);
      case MissingServletRequestParameterException he ->
        handleMissingServletRequestParameterException(request, response, he);
      case ServletRequestBindingException he -> handleServletRequestBindingException(request, response, he);
      case ConversionNotSupportedException he -> handleConversionNotSupportedException(request, response, he);
      case TypeMismatchException he -> handleTypeMismatchException(request, response, he);
      default -> null;
    };
  }

  /**
   * 处理参数绑定异常
   * <p>
   * 将验证错误信息提取为 Map，格式为 {字段名: 错误消息}。
   */
  private ModelAndView handleBindException(HttpServletRequest request,
                                           HttpServletResponse response,
                                           BindException exception) {
    Map<String, String> allErrors = new LinkedHashMap<>();
    exception.getBindingResult().getAllErrors().forEach(err -> {
      if (err instanceof FieldError fe) {
        allErrors.put(fe.getField(), fe.getDefaultMessage());
      } else {
        allErrors.put(err.getObjectName(), err.getDefaultMessage());
      }
    });
    Map<String, Object> data = Map.of("validator", allErrors);
    responseWithData(HttpStatus.BAD_REQUEST, data, request, response);
    return new ModelAndView();
  }

  /**
   * 处理无处理器异常（404）
   */
  private ModelAndView handleNoHandlerFoundException(HttpServletRequest request, HttpServletResponse response) {
    responseNoData(HttpStatus.NOT_FOUND, request, response);
    return new ModelAndView();
  }

  /**
   * 处理请求方法不支持异常
   * <p>
   * 返回支持的方法列表。
   */
  private ModelAndView handleRequestMethodNotSupportedException(HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                HttpRequestMethodNotSupportedException exception) {
    String[] supportedMethods = exception.getSupportedMethods();
    if (supportedMethods != null) {
      responseWithData(HttpStatus.METHOD_NOT_ALLOWED, Arrays.asList(supportedMethods), request, response);
    } else {
      responseNoData(HttpStatus.METHOD_NOT_ALLOWED, request, response);
    }
    return new ModelAndView();
  }

  private ModelAndView handleMediaTypeNotSupportedException(HttpServletRequest request,
                                                            HttpServletResponse response,
                                                            HttpMediaTypeNotSupportedException exception) {
    responseWithMessage(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  private ModelAndView handleMissingPathVariableException(HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          MissingPathVariableException exception) {
    responseWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  private ModelAndView handleMissingServletRequestParameterException(HttpServletRequest request,
                                                                     HttpServletResponse response,
                                                                     MissingServletRequestParameterException exception) {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  private ModelAndView handleServletRequestBindingException(HttpServletRequest request,
                                                            HttpServletResponse response,
                                                            ServletRequestBindingException exception) {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  private ModelAndView handleConversionNotSupportedException(HttpServletRequest request,
                                                             HttpServletResponse response,
                                                             ConversionNotSupportedException exception) {
    responseWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  private ModelAndView handleTypeMismatchException(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   TypeMismatchException exception) {
    responseWithMessage(HttpStatus.BAD_REQUEST, exception.getMessage(), request, response);
    return new ModelAndView();
  }

  /**
   * 使用消息创建响应
   */
  private void responseWithMessage(HttpStatus status,
                                   String message,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
    Response respData = new Response(status.value(), message != null ? message : status.getReasonPhrase(), null, null);
    try {
      out(respData, request, response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 创建无数据的响应
   */
  private void responseNoData(HttpStatus status,
                              HttpServletRequest request,
                              HttpServletResponse response) {
    Response respData = new Response(status.value(), status.getReasonPhrase(), null, null);
    try {
      out(respData, request, response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 创建带数据的响应
   */
  private void responseWithData(HttpStatus status,
                                Object data,
                                HttpServletRequest request,
                                HttpServletResponse response) {
    Response respData = new Response(status.value(), status.getReasonPhrase(), null, data);
    try {
      out(respData, request, response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 输出响应
   * <p>
   * 根据 Accept Header 选择合适的 HttpMessageConverter 进行序列化。
   * 默认使用 application/json。
   */
  private void out(Object data,
                   HttpServletRequest request,
                   HttpServletResponse response) throws IOException {
    String accept = request.getHeader("Accept");
    MediaType mediaType;
    if (Strings.isBlank(accept)) {
      mediaType = MediaType.APPLICATION_JSON;
    } else {
      mediaType = MediaType.parseMediaType(accept);
    }
    for (HttpMessageConverter<Object> converter : converters) {
      if (converter instanceof GenericHttpMessageConverter<Object> genericConverter) {
        if (genericConverter.canWrite(RESPONSE_TYPE, RESPONSE_TYPE, mediaType)) {
          var outMessage = new ServletServerHttpResponse(response);
          genericConverter.write(data, RESPONSE_TYPE, mediaType, outMessage);
          return;
        }
      } else if (converter.canWrite(RESPONSE_TYPE, mediaType)) {
        var outMessage = new ServletServerHttpResponse(response);
        converter.write(data, mediaType, outMessage);
        return;
      }
    }
  }
}
