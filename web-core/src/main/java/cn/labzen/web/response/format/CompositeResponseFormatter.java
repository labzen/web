package cn.labzen.web.response.format;

import cn.labzen.web.api.response.Response;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.ServiceLoader;

public class CompositeResponseFormatter implements ResponseFormatter {

  private final List<ResponseFormatter> formatters;

  public CompositeResponseFormatter() {
    // 第1 处理已经是 Response 结构化的情况，快速返回已经是 Response 结构的情况
    ResponseAgainResponseFormatter responseAgainRF = new ResponseAgainResponseFormatter();
    // 第2 格式化不正常的 Http Status 结果，如404
    AbnormalStatusResponseFormatter abnormalStatusRF = new AbnormalStatusResponseFormatter();
    // 倒2 处理 Result 中的返回值格式化，标准的返回结构
    StandardResultResponseFormatter standardResultRF = new StandardResultResponseFormatter();
    // 倒1 处理前面所有格式化器都未考虑到的请
    UnexpectedResponseFormatter unexpectedRF = new UnexpectedResponseFormatter();

    List<ResponseFormatter> allFormatters = Lists.newArrayList(responseAgainRF, abnormalStatusRF);
    ServiceLoader<ResponseFormatter> loadedFormatters = ServiceLoader.load(ResponseFormatter.class, this.getClass().getClassLoader());
    for (ResponseFormatter loadedFormatter : loadedFormatters) {
      allFormatters.add(loadedFormatter);
    }
    allFormatters.add(standardResultRF);
    allFormatters.add(unexpectedRF);

    this.formatters = List.copyOf(allFormatters);
  }

  @Override
  public boolean support(Class<?> clazz, HttpServletRequest request) {
    return true;
  }

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
