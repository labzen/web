package cn.labzen.web.api.paging;

/**
 * 分页排序条件。
 *
 * @param name  排序字段名（表字段或映射类属性）
 * @param asc   是否升序（true 为升序，false 为降序）
 * @param nulls 空值排序策略，仅支持 "first" 或 "last"（不区分大小写），用于兼容支持 NULLS FIRST/LAST 语法的数据库
 */
public record Order(String name, boolean asc, String nulls) {
}
