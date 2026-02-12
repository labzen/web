package cn.labzen.web.api.response;

/**
 * 缓存相关
 *
 * @param key        缓存key，用于区分缓存信息
 * @param expiration 缓存过期时间，用于前端判断获取到的数据可信时间周期
 */
public record Cache(String key, String expiration) {
}
