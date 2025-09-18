package cn.labzen.web.response.bean;

/**
 * 安全相关
 *
 * @param encryption 是否加密了response data，todo 后期实现可加入公钥等信息
 * @param checksum   返回信息摘要，用于快速校验数据完整性，防止信息篡改
 */
public record Security(boolean encryption, String checksum) {
}
