package cn.labzen.web.ap.internal

import cn.labzen.meta.Labzens
import cn.labzen.meta.component.ComponentRecorder
import cn.labzen.meta.configuration.ConfigurationReader
import cn.labzen.web.meta.WebConfiguration

@Deprecated("")
class MetaUtils private constructor() {

  companion object {

    private lateinit var configuration: WebConfiguration

    // todo 可能读不到，因为labzen.yml在另一个module中定义，需要想个办法
    fun initialize() {
      ComponentRecorder.record()
      ConfigurationReader.read()

      configuration = Labzens.configurationWith(WebConfiguration::class.java)
    }

    fun configuration() = configuration
  }
}