package cn.labzen.web.response.bean;

/**
 * 通用Response数据结构，主要适用于Http，也可转为json用于TCP等协议
 *
 * @param code    返回结果编码，用来标识返回结果的内容状态。主要用于HTTP状态码的补充，如请求结果为常见状态，可忽略
 * @param message 请求处理状态的描述信息，对于成功请求，可以是空字符串；传递一个前后端统一的 message code 来做国际化，是好的实践
 * @param meta    元信息
 * @param data    返回数据
 */
public record Response(int code, String message, Meta meta, Object data) {
}
