package cn.labzen.web.response.bean

import cn.labzen.web.paging.Pagination
import cn.labzen.web.response.HttpStatusExt
import org.springframework.http.HttpStatus

object Results {

  @JvmStatic
  fun justSuccess() =
    Result(200)

  @JvmStatic
  fun justFailure() =
    Result(500)

  /* ======================== with status ======================== */

  @JvmStatic
  fun justStatus(status: Int) =
    Result(status)

  @JvmStatic
  fun justStatus(status: HttpStatus) =
    Result(status.value())

  @JvmStatic
  fun justStatus(status: HttpStatusExt) =
    Result(status.code)

  /* ======================== with value ======================== */

  @JvmStatic
  fun with(value: Any) =
    Result(200, value)

  @JvmStatic
  fun with(status: Int, value: Any) =
    Result(status, value)

  @JvmStatic
  fun with(status: HttpStatus, value: Any) =
    Result(status.value(), value)

  @JvmStatic
  fun with(status: HttpStatusExt, value: Any) =
    Result(status.code, value)

  @JvmStatic
  fun with(status: Int, value: Any, message: String) =
    Result(status, value, message)

  @JvmStatic
  fun with(status: HttpStatus, value: Any, message: String) =
    Result(status.value(), value, message)

  @JvmStatic
  fun with(status: HttpStatusExt, value: Any, message: String) =
    Result(status.code, value, message)

  /* ======================== with message ======================== */

  @JvmStatic
  fun withMessage(message: String) =
    Result(200, message = message)

  @JvmStatic
  fun withMessage(status: Int, message: String) =
    Result(status, message = message)

  @JvmStatic
  fun withMessage(status: HttpStatus, message: String) =
    Result(status.value(), message = message)

  @JvmStatic
  fun withMessage(status: HttpStatusExt, message: String) =
    Result(status.code, message = message)

  /* ======================== with paging records ======================== */

  @JvmStatic
  fun asPaging(records: List<*>, pageNumber: Int, pageSize: Int, totalOfRecords: Long? = null): Result {
    val totalOfPages = totalOfRecords?.let {
      it / pageSize + if (it % pageSize == 0L) 0 else 1
    } ?: 0
    val pagination = Pagination(pageNumber, pageSize, totalOfRecords ?: 0, totalOfPages, records)
    return Result(200, pagination)
  }
}