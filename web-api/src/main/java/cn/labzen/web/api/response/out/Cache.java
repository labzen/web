package cn.labzen.web.api.response.out;

/**
 * 缓存信息。
 *
 * @param key        缓存键，用于区分不同的缓存信息
 * @param expiration 缓存过期时间，用于前端判断数据的可信时间周期
 */
public record Cache(String key, String expiration) {
}
