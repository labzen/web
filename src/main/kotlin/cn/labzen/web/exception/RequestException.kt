package cn.labzen.web.exception

import cn.labzen.meta.exception.LabzenRuntimeException

class RequestException : LabzenRuntimeException {

  internal val code: Int

  @JvmOverloads
  constructor(code: Int = 500, message: String) : super(message) {
    this.code = code
  }

  @JvmOverloads
  constructor(code: Int = 500, message: String, vararg arguments: Any?) : super(message, *arguments) {
    this.code = code
  }

  @JvmOverloads
  constructor(code: Int = 500, cause: Throwable) : super(cause) {
    this.code = code
  }

  @JvmOverloads
  constructor(code: Int = 500, cause: Throwable, message: String) : super(cause, message) {
    this.code = code
  }

  @JvmOverloads
  constructor(code: Int = 500, cause: Throwable, message: String, vararg arguments: Any?) : super(
    cause,
    message,
    *arguments
  ) {
    this.code = code
  }
}
