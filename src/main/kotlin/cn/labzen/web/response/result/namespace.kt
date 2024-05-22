@file:Suppress("DataClassPrivateConstructor")

package cn.labzen.web.response.result

import cn.labzen.web.response.HttpStatusExt
import cn.labzen.web.response.Pagination
import org.springframework.http.HttpStatus

data class Result private constructor(
  val code: Int,
  val value: Any?,
  val pagination: Pagination?,
  val message: String?
) {
  companion object {

    /**
     * 无返回值
     */
    @JvmStatic
    fun justSuccess() =
      with(value = null)

    /**
     * 指定状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(status: Int, value: Any? = null, message: String? = null) =
      Result(status, value, null, message)

    /**
     * 指定状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(status: Int, values: Collection<*>, pagination: Pagination? = null, message: String? = null) =
      Result(status, values, pagination, message)

    /**
     * 指定状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(
      status: Int,
      values: Collection<*>,
      page: Int,
      size: Int,
      recordCount: Long,
      message: String? = null
    ): Result {
      val pageCount = recordCount / size + if (recordCount % size == 0L) 0 else 1
      val pagination = Pagination(page, size, recordCount, pageCount)
      return Result(status, values, pagination, message)
    }

    /**
     * 指定扩展响应状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(responseStatus: HttpStatusExt, value: Any? = null, message: String? = null) =
      Result(responseStatus.code, value, null, message)

    /**
     * 指定扩展响应状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(
      responseStatus: HttpStatusExt,
      values: Collection<*>,
      pagination: Pagination? = null,
      message: String? = null
    ) =
      Result(responseStatus.code, values, pagination, message)

    /**
     * 指定扩展响应状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(
      responseStatus: HttpStatusExt,
      values: Collection<*>,
      page: Int,
      size: Int,
      recordCount: Long,
      message: String? = null
    ): Result {
      val pageCount = recordCount / size + if (recordCount % size == 0L) 0 else 1
      val pagination = Pagination(page, size, recordCount, pageCount)
      return Result(responseStatus.code, values, pagination, message)
    }

    /**
     * 指定标准响应状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(responseStatus: HttpStatus, value: Any? = null, message: String? = null) =
      Result(responseStatus.value(), value, null, message)

    /**
     * 指定标准响应状态
     */
    @JvmOverloads
    @JvmStatic
    fun withStatus(
      responseStatus: HttpStatus,
      values: Collection<*>,
      page: Int,
      size: Int,
      recordCount: Long,
      message: String? = null
    ): Result {
      val pageCount = recordCount / size + if (recordCount % size == 0L) 0 else 1
      val pagination = Pagination(page, size, recordCount, pageCount)
      return Result(responseStatus.value(), values, pagination, message)
    }

    /**
     * 默认 200 状态
     */
    @JvmOverloads
    @JvmStatic
    fun with(value: Any?, message: String? = null) =
      Result(200, value, null, message)

    /**
     * 默认 200 状态
     */
    @JvmOverloads
    @JvmStatic
    fun with(values: Collection<*>, pagination: Pagination? = null, message: String? = null) =
      Result(200, values, pagination, message)

    /**
     * 普通返回值
     */
    @JvmOverloads
    @JvmStatic
    fun with(
      values: Collection<*>,
      page: Int,
      size: Int,
      recordCount: Long,
      message: String? = null
    ): Result {
      val pageCount = recordCount / size + if (recordCount % size == 0L) 0 else 1
      val pagination = Pagination(page, size, recordCount, pageCount)
      return Result(200, values.toList(), pagination, message)
    }
  }
}
