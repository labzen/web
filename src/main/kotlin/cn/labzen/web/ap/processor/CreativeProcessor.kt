package cn.labzen.web.ap.processor

import cn.labzen.web.ap.internal.ClassCreator
import cn.labzen.web.ap.internal.context.ControllerContext
import cn.labzen.web.ap.processor.InternalProcessor.Companion.PRIORITY_CREATIVE

class CreativeProcessor : InternalProcessor {

  override fun process(context: ControllerContext) {
    ClassCreator(context.root, context.apc.filer).create()
  }

  override fun priority(): Int = PRIORITY_CREATIVE
}