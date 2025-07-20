package cn.labzen.web.annotation

/**
 * 标识该 ***接口*** 作为 Labzen Web 的 Controller 使用
 *
 * 通过接口的形式定义 Spring API，可保证业务逻辑代码不会出现在 controller 层，专注于接口职责，结构清晰，职责单一。
 *
 * 在接口中定义API，与正规的 Controller 类定义相同，包括路径定义、参数校验、鉴权注解等。在编译代码时，将会生成对应的 Controller 类，开发者可在 `target/generated-sources` 目录中查看。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class LabzenController()
