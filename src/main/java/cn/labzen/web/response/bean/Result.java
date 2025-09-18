package cn.labzen.web.response.bean;

/**
 * Labzen Web 组件标准响应返回信息
 *
 * @param code    响应状态码
 * @param value   响应数据
 * @param message 响应信息
 */
public record Result(int code, Object value, String message) {
}
