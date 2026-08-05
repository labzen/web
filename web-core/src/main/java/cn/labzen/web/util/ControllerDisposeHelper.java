package cn.labzen.web.util;

import cn.labzen.tool.util.DateTimes;
import cn.labzen.tool.util.Strings;
import com.google.common.primitives.Longs;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.DigestUtils;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class ControllerDisposeHelper {

  /**
   * 请求时间属性键（格式：yyyy-MM-dd HH:mm:ss）
   */
  private static final String REST_REQUEST_TIME_STRING = "labzen.web.runtime.request.time.string";
  /**
   * 请求时间属性键（毫秒）
   */
  private static final String REST_REQUEST_TIME_MILLIS = "labzen.web.runtime.request.time.millis";

  private ControllerDisposeHelper() {
  }

  /**
   * 计算输入字符串的 MD5 哈希并返回前 5 位十六进制字符串。
   *
   * @param methodName     方法名
   * @param returnType     返回类型
   * @param parameterTypes 参数类型列表
   */
  public static String hashControllerMethod(String methodName, String returnType, List<String> parameterTypes) {
    String parameters = String.join(", ", parameterTypes);
    String signature = String.format("%s %s(%s)", returnType, methodName, parameters);
    byte[] bytes = signature.getBytes();
    byte[] digest = DigestUtils.md5Digest(bytes);
    String hash = HexFormat.of().formatHex(digest);
    return Strings.sub(hash, 0, 5);
  }

  /**
   * 记录请求开始时间
   */
  public static void recordRequestTime(HttpServletRequest request) {
    request.setAttribute(REST_REQUEST_TIME_MILLIS, System.currentTimeMillis());
    request.setAttribute(REST_REQUEST_TIME_STRING, DateTimes.formattedNow());
  }

  /**
   * 获取请求开始时间字符串
   */
  public static String getRequestTime(HttpServletRequest request) {
    return Strings.value(request.getAttribute(REST_REQUEST_TIME_STRING), "");
  }

  /**
   * 计算请求执行时长（毫秒）
   */
  public static long calculateExecutionTime(HttpServletRequest request) {
    Object millsAttr = request.getAttribute(REST_REQUEST_TIME_MILLIS);
    String requestMillsStr = Strings.value(millsAttr, "0");
    long requestMills = Optional.ofNullable(Longs.tryParse(requestMillsStr)).orElse(0L);
    return System.currentTimeMillis() - requestMills;
  }
}
