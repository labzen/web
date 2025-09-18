package cn.labzen.web.paging;

/**
 * 分页的 order by 条件
 *
 * @param name  排序依据 表字段/映射类属性
 * @param asc   asc or desc
 * @param nulls 为兼容支持 NULLS FIRST|LAST 语法的数据库，只接受 "first" 或 "last" 不区分大小写
 */
public record Order(String name, boolean asc, String nulls) {
}
