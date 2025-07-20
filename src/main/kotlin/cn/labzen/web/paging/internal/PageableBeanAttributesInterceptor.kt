package cn.labzen.web.paging.internal

import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import java.lang.reflect.Method

class PageableBeanAttributesInterceptor(private val target: Any) {

  @RuntimeType
  fun intercept(@This proxy: Any, @Origin method: Method, @AllArguments args: Array<Any?>): Any? {
    return method.invoke(target, *args)
  }
}