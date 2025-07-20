package cn.labzen.web.ap.processor

import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.internal.element.ElementClass
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_READ_SOURCE

class ReadSourceProcessor : InternalProcessor {

  override fun process(context: ControllerContext) {
    with(context) {
      val className = source.simpleName.toString() + apc.config.classNameSuffix()
      val pkg = apc.elementUtils.getPackageOf(source).qualifiedName.toString()

      val implements = source.asType()

      root = ElementClass(className, pkg, implements)
    }
  }

  override fun priority(): Int = PRIORITY_READ_SOURCE
}