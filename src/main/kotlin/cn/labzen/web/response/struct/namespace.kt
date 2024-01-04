package cn.labzen.web.response.struct

import cn.labzen.web.response.Pagination

/**
 * 通用Response数据结构，主要适用于Http，也可转为json用于TCP等协议
 *
 * @param code 返回结果编码，用来标识返回结果的内容状态。主要用于HTTP状态码的补充，如请求结果为常见状态，可忽略
 * @param message 请求处理状态的描述信息，对于成功请求，可以是空字符串；传递一个前后端统一的 message code 来做国际化，是好的实践
 * @param meta 元信息
 * @param data 返回数据
 */
data class Response(
  val code: Int,
  val message: String,
  val meta: Meta? = null,
  val data: Any? = null
)

/**
 * 存放与请求相关的元数据
 *
 * @param requestTime 服务器接收到请求的时间
 * @param executionTime 服务器处理请求花费的时间
 * @param pagination 分页
 * @param cache 缓存
 */
data class Meta(
  val requestTime: String? = null,
  val executionTime: Long = 0,
  val pagination: Pagination? = null,
  val cache: Cache? = null,
  val security: Security? = null
)

/**
 * 缓存相关
 *
 * @param key 缓存key，用于区分缓存信息
 * @param expiration 缓存过期时间，用于前端判断获取到的数据可信时间周期
 */
data class Cache(
  val key: String,
  val expiration: String
)

/**
 * 安全相关
 *
 * @param encryption 是否加密了response data，todo 后期实现可加入公钥等信息
 * @param checksum 返回信息摘要，用于快速校验数据完整性，防止信息篡改
 */
data class Security(
  val encryption: Boolean,
  val checksum: String
)

data class ExceptionEntity(val code: Int, val message: String)