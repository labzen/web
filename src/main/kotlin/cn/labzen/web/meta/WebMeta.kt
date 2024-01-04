package cn.labzen.web.meta

import cn.labzen.meta.component.LabzenComponent

class WebMeta : LabzenComponent {
  override fun description(): String =
    "对WEB层做的极简开发优化，防止该层代码掺杂业务逻辑代码，避免造成臃肿"

  override fun mark(): String =
    "Labzen-Web"

  override fun packageBased(): String =
    "cn.labzen.web"
}