package cn.labzen.web.response;

/**
 * HTTP Response（状态）增量定义，描述一个请求的返回状态，不覆盖标准HTTP状态
 */
public record HttpStatusExt(int code, String description) {

  /* ==========  1XX 临时信息，标记请求在被业务代码真正的处理之前的状态  ========== */
  /**
   * 用于被IP黑名单限制拦截下的请求访问
   */
  public static final HttpStatusExt IP_BLACKLIST = new HttpStatusExt(180, "IP受到访问（黑名单）限制");

  /**
   * 用于在某些比较特殊、敏感等具体访问次数限制的接口，当同一IP在时间段内，访问的次数超过一定的阈值，可以定义不同的时间段长度、访问的次数
   */
  public static final HttpStatusExt IP_TOO_OFTEN = new HttpStatusExt(181, "IP访问过于频繁");

  /* ==========  2XX 成功信息  ========== */
  /**
   * 用于耗时较长的异步访问，带有该状态的响应内容，为下次拉取请求结果的唯一有效证据。（该状态可以使用202 Accepted替代）
   */
  public static final HttpStatusExt APPENDING = new HttpStatusExt(280, "已接受，但需要客户端再次访问拉取结果");

  /* ==========  3XX 重定向  ========== */
  /**
   * 用于当请求访问的资源，可以被当前服务器处理（或于其他的服务器一起处理、转交处理），最终的响应资源，需要到其他的服务器获取的特殊情况下
   */
  public static final HttpStatusExt GET_FROM_ANOTHER = new HttpStatusExt(380, "请求的资源存在于另一个位置");

  /* ==========  4XX 客户端错误  ========== */

  /**
   * 访问API的凭证并非是服务器颁发，或凭证已过期。（该状态是401的细化）
   */
  public static final HttpStatusExt USELESS_PROOF = new HttpStatusExt(480, "使用非法的API访问凭证");

  /**
   * 访问API的凭证，对于当前请求的资源，不具备权限。（该状态是401的细化）
   */
  public static final HttpStatusExt NO_PERMISSION = new HttpStatusExt(481, "使用的API访问凭证无权限");

  /**
   * 提交的参数（query string 或 form data）校验失败
   */
  public static final HttpStatusExt INVALID_PARAMETER = new HttpStatusExt(482, "无效的参数");

  /**
   * 在某些需要使用签名保证请求的真实性的情况，验证签名错误
   */
  public static final HttpStatusExt INVALID_SIGNATURE = new HttpStatusExt(483, "无效签名");

  /* ==========  5** 服务器错误，必须带有错误信息  ========== */
  /**
   * 一般为调用三方服务失败或异常，这里不包括请求三方代码的异常
   */
  public static final HttpStatusExt THIRD_PARTY_ERROR = new HttpStatusExt(580, "三方系统错误");

  /**
   * 代码异常，不提倡直接将500返回给客户端
   */
  public static final HttpStatusExt UNEXPECTED_ERROR = new HttpStatusExt(599, "无法确定的内部错误");

}
