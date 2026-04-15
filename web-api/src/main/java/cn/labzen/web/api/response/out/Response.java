package cn.labzen.web.api.response.out;

/**
 * 通用响应数据结构。
 * <p>
 * 适用于 HTTP 响应，也可转换为 JSON 用于 TCP 等协议。
 *
 * @param code    响应状态码，用于补充 HTTP 状态码（如请求成功可忽略）
 * @param message 响应消息，对于成功请求可为空字符串；建议使用国际化消息码
 * @param meta    元信息
 * @param data    返回数据
 */
public record Response(int code, String message, Meta meta, Object data) {
}
