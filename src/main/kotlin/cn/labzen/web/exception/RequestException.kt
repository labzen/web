package cn.labzen.web.exception

import cn.labzen.meta.exception.LabzenRuntimeException

open class RequestException : LabzenRuntimeException {

  internal val code: Int
  internal val logging: Boolean

  @JvmOverloads
  constructor(code: Int = 500, logging: Boolean = true, message: String) : super(message) {
    this.code = code
    this.logging = logging

  }

  @JvmOverloads
  constructor(code: Int = 500, logging: Boolean = true, message: String, vararg arguments: Any?) : super(
    message,
    *arguments
  ) {
    this.code = code
    this.logging = logging

  }

  @JvmOverloads
  constructor(code: Int = 500, logging: Boolean = true, cause: Throwable) : super(cause) {
    this.code = code
    this.logging = logging

  }

  @JvmOverloads
  constructor(code: Int = 500, logging: Boolean = true, cause: Throwable, message: String) : super(
    cause,
    message
  ) {
    this.code = code
    this.logging = logging

  }

  @JvmOverloads
  constructor(
    code: Int = 500,
    logging: Boolean = true,
    cause: Throwable,
    message: String,
    vararg arguments: Any?
  ) : super(
    cause,
    message,
    *arguments
  ) {
    this.code = code
    this.logging = logging

  }
}
