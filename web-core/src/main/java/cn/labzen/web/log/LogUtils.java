package cn.labzen.web.log;

import cn.labzen.tool.util.Strings;
import org.springframework.util.DigestUtils;

import java.util.HexFormat;
import java.util.List;

public final class LogUtils {

  private LogUtils() {
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
}
