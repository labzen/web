package cn.labzen.web.api.response.result;

/**
 * 通用值响应结果。
 * <p>
 * 用于返回带有状态码、消息和数据的标准响应。
 *
 * @param code    响应状态码
 * @param value   响应数据
 * @param message 响应消息
 */
public record ValueResult(int code, Object value, String message) implements Result {
}
