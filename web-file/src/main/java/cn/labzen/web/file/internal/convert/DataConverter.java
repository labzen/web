package cn.labzen.web.file.internal.convert;

/**
 * todo 暂时先不实现，待定；导出和导入的转换逻辑还没考虑明白
 * @param <S>
 * @param <T>
 */
@Deprecated
public interface DataConverter<S, T> {

  T convert(S source);

  S restore(T target);
}
