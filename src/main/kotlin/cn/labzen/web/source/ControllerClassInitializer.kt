package cn.labzen.web.source

import cn.labzen.web.meta.WebConfiguration
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController

internal object ControllerClassInitializer {

  val controllerClasses = mutableSetOf<Class<*>>()

  fun scanAndGenerate(configuration: WebConfiguration) {
    val controllerPackage = configuration.controllerPackage()
    val controllerInterfaces = scanInterfaces(controllerPackage)

    val controllerClasses = controllerInterfaces.map {
      ControllerGenerator(it, configuration).generate()
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
    return allControllers.filter { it.isInterface }
  }
}