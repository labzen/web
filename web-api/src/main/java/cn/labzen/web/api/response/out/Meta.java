package cn.labzen.web.api.response.out;

import cn.labzen.web.api.paging.Pagination;

/**
 * 响应元信息。
 * <p>
 * 存放与请求相关的元数据，包括请求时间、处理时间、分页信息、缓存信息和安全信息。
 *
 * @param requestTime   服务器接收到请求的时间
 * @param executionTime 服务器处理请求花费的时间（毫秒）
 * @param pagination    分页信息
 * @param cache         缓存信息
 * @param security      安全信息
 */
public record Meta(String requestTime, Long executionTime, Pagination<?> pagination, Cache cache, Security security) {
}
