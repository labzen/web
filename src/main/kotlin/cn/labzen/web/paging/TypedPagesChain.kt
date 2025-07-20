package cn.labzen.web.paging

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class TypedPagesChain<T>(
  private val source: Pageable,
  private val raw: Class<T>
) {

  private val args = mutableListOf<Type>()

  fun with(vararg args: Class<*>): TypedPagesChain<T> {
    this.args.addAll(args)
    return this
  }

  fun convert(): T? {
    val type: Type = if (args.isEmpty()) {
      raw
    } else {
      object : ParameterizedType {
        override fun getRawType(): Type = raw
        override fun getActualTypeArguments(): Array<Type> = args.toTypedArray()
        override fun getOwnerType(): Type? = null
      }
    }
    return source.to(type)
  }
}