package cn.labzen.web.response.bean;

import cn.labzen.web.paging.Pagination;

/**
 * 存放与请求相关的元数据
 *
 * @param requestTime   服务器接收到请求的时间
 * @param executionTime 服务器处理请求花费的时间
 * @param pagination    分页
 * @param cache         缓存相关
 * @param security      安全相关
 */
public record Meta(String requestTime, Long executionTime, Pagination<?> pagination, Cache cache, Security security) {
}
