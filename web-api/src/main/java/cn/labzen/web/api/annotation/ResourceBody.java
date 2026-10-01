package cn.labzen.web.api.annotation;

import java.lang.annotation.*;

/**
 * 标准资源入参标注注解。
 * <p>
 * 标注在 Controller 接口方法的资源参数上，表示该参数的绑定结构由框架统一决定：
 * 由 {@code labzen.web.config} 中的 {@code processor.resource-binding} 配置（可选值 {@code FORM}、{@code JSON}，
 * 默认 {@code FORM}）决定在生成的 Controller 实现类中落为 {@code @ModelAttribute} 还是 {@code @RequestBody}。
 * <p>
 * 因此，同一个 Controller 接口可以在不改动任何代码的前提下，通过配置切换资源入参的传递方式
 * （{@code application/x-www-form-urlencoded} 或 {@code application/json}）。
 * <p>
 * 本注解在编译期参与代码生成，生成的实现类中不会保留本注解。由于其常随框架提供的基础接口
 * （如 {@link cn.labzen.web.api.controller.StandardController}）一起以 class 形式被继承，
 * 因此采用 {@link RetentionPolicy#CLASS} 保留策略，确保注解处理器在读取继承自 class 的接口方法时仍能获取到它。
 * <p>
 * 若参数上同时显式声明了 Spring 的绑定注解（如 {@code @RequestBody}、{@code @ModelAttribute}、
 * {@code @RequestParam}、{@code @PathVariable} 等），则以显式声明为准，本注解不生效。
 * <p>
 * GET 请求入口不存在请求体，其参数不应使用本注解；若使用，编译期会给出告警。
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface ResourceBody {

}
