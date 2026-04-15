package cn.labzen.web.api.response.out;

/**
 * 安全信息。
 *
 * @param encryption 是否对响应数据进行了加密
 * @param checksum   返回信息摘要，用于快速校验数据完整性，防止信息篡改
 */
public record Security(boolean encryption, String checksum) {
}
