package cn.labzen.web.ap.service;

import cn.labzen.web.paging.Pageable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MenuDto implements Pageable, Cloneable, Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;
  /**
   * 前端元素资源编码
   */
  private String code;
  /**
   * 上级元素资源编码，关联code字段
   */
  private String parentCode;
  /**
   * 权限code，与permission表code一致
   */
  private String permissionCode;
  /**
   * 元素标题文本，仅供后台管理员在管理菜单时显示
   */
  private String title;
  /**
   * 前端VUE路由名称，对应vue页面的export default{ name: '' }，以及vue路由配置中的name
   */
  private String routeName;
  /**
   * 元素排列顺序
   */
  private Integer routeRank;
  /**
   * 菜单图标，如果是页面内元素，不需要设置
   */
  private String routeIcon;
  /**
   * 菜单元素路由的本地化local key，用于显示在breadcrumb中，以及菜单等位置
   */
  private String locale;
  /**
   * 缓存页面
   */
  private Boolean cacheable = true;
  /**
   * 元素是否有效
   */
  private Boolean active = true;
  /**
   * 资源地址（供外部链接使用）
   */
  private String uri;
  /**
   * 页面元素资源描述
   */
  private String description;
  /**
   * 子集合
   */
  private List<MenuDto> children;

  @Override
  public MenuDto clone() throws CloneNotSupportedException {
    MenuDto cloned = (MenuDto) super.clone();

    if (this.children != null) {
      cloned.children = new ArrayList<>();
      for (MenuDto sub : this.children) {
        cloned.children.add(sub.clone());
      }
    }

    return cloned;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getParentCode() {
    return parentCode;
  }

  public void setParentCode(String parentCode) {
    this.parentCode = parentCode;
  }

  public String getPermissionCode() {
    return permissionCode;
  }

  public void setPermissionCode(String permissionCode) {
    this.permissionCode = permissionCode;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getRouteName() {
    return routeName;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  public Integer getRouteRank() {
    return routeRank;
  }

  public void setRouteRank(Integer routeRank) {
    this.routeRank = routeRank;
  }

  public String getRouteIcon() {
    return routeIcon;
  }

  public void setRouteIcon(String routeIcon) {
    this.routeIcon = routeIcon;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Boolean getCacheable() {
    return cacheable;
  }

  public void setCacheable(Boolean cacheable) {
    this.cacheable = cacheable;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<MenuDto> getChildren() {
    return children;
  }

  public void setChildren(List<MenuDto> children) {
    this.children = children;
  }
}
