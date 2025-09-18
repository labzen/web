package cn.labzen.web.ap.service;

import cn.labzen.web.paging.Pageable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class MenuDto implements Pageable, Cloneable, Serializable {

  @Serial
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

}
