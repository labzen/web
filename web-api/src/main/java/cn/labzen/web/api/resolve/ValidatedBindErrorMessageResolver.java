package cn.labzen.web.api.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.validation.ObjectError;

/**
 * 参数校验失败时的错误消息解析器。
 * <p>
 * 允许上层业务项目自定义 BindException 中各字段的错误消息，替代 Spring 校验注解中的默认消息。
 * 上层项目通过 SPI 机制提供实现，即可介入校验失败消息的生成。
 * <p>
 * 典型使用场景：
 * <ul>
 *   <li>根据校验注解类型（如 {@code @NotBlank}、{@code @Size}）返回不同的业务提示</li>
 *   <li>根据字段名返回字段对应的中文名或 i18n 键</li>
 *   <li>根据校验失败的错误码（code）做精细化提示</li>
 *   <li>根据目标校验类型做差异化处理</li>
 * </ul>
 * <p>
 * 若未通过 SPI 提供实现，则回退到 {@link ObjectError#getDefaultMessage()} 作为兜底。
 */
@FunctionalInterface
public interface ValidatedBindErrorMessageResolver {

  /**
   * 根据校验失败的字段错误信息，解析出最终返回给客户端的错误消息。
   *
   * @param objectError 校验失败的字段错误对象，包含字段名、校验码、默认消息等完整信息
   * @param targetType  被校验的目标对象类型；若无法获取目标对象则为 {@code null}
   * @return 解析后的错误消息；若返回 {@code null}，则使用 {@link ObjectError#getDefaultMessage()} 兜底
   */
  @Nullable
  String resolve(@Nonnull ObjectError objectError, @Nullable Class<?> targetType);
}
