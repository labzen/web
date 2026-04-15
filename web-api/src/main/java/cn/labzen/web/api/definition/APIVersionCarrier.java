package cn.labzen.web.api.definition;

/**
 * API 版本携带方式枚举。
 * <p>
 * 定义了 API 版本信息可以通过何种方式传递给服务端。
 */
public enum APIVersionCarrier {

  /**
   * 禁用版本控制
   */
  DISABLE,

  /**
   * 通过请求头传递版本信息
   */
  HEADER,

  /**
   * 通过 URI 路径传递版本信息
   */
  URI,

  /**
   * 通过请求参数传递版本信息
   */
  PARAMETER
}
