package cn.labzen.web.ap.exception

class TypeNotAvailableException(val fqcn: String, ex: Throwable) : ReflectiveOperationException(ex)