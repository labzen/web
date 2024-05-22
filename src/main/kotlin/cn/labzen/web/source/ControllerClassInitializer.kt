package cn.labzen.web.source

import cn.labzen.web.annotation.ServiceHandler
import cn.labzen.web.meta.WebConfiguration
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController

internal object ControllerClassInitializer {

  /**
   * 生成的Controller实现类将暂存在这里
   */
  val controllerClasses = mutableSetOf<Class<*>>()

  /**
   * 扫描所有的Controller接口，并根据接口生成Controller实现类
   */
  fun scanAndGenerate(configuration: WebConfiguration) {
    val controllerPackage = configuration.controllerPackage()
    val controllerInterfaces = scanInterfaces(controllerPackage)

    val controllerClasses = controllerInterfaces.map {
      ControllerGenerator(configuration, it).generate()
    }
    this.controllerClasses.addAll(controllerClasses)
  }

  private fun scanInterfaces(controllerPackage: String?): List<Class<*>> {
    val configurationBuilder = ConfigurationBuilder()
      .forPackages(controllerPackage ?: "")
      .addScanners(Scanners.TypesAnnotated)
    val reflections = Reflections(configurationBuilder)
    val allControllers = reflections.getTypesAnnotatedWith(Controller::class.java)
    allControllers.addAll(reflections.getTypesAnnotatedWith(RestController::class.java))

    // 必须符合：1. Spring Controller/RestController；2. 是接口；3. 注解了 ServiceHandler
    return allControllers.filter {
      it.isInterface && it.isAnnotationPresent(ServiceHandler::class.java)
    }
  }
}