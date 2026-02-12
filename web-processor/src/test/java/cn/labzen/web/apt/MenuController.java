package cn.labzen.web.apt;

import cn.labzen.web.api.annotation.Abandoned;
import cn.labzen.web.api.annotation.Call;
import cn.labzen.web.api.annotation.MappingVersion;
import cn.labzen.web.api.controller.StandardController;
import cn.labzen.web.api.response.Result;
import cn.labzen.web.apt.service.MenuDto;
import cn.labzen.web.apt.service.MenuRealm;
import cn.labzen.web.apt.service.RoleRealm;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;

@Validated
@RestController("abc")
@RequestMapping(value = "system/demo/test", produces = {"application/vnd.app.v1+json"})
//@LabzenController
public interface MenuController extends StandardController<MenuRealm, MenuDto, Long> {

  @Override
  @MappingVersion(4)
  @Call(target = RoleRealm.class, method = "add")
  @Nonnull
  Result create(MenuDto resource);

  @Nonnull
  @Override
  @Abandoned
  Result remove(Long id);

  /**
   * 这里定义了一个快速入口
   */
  @PostMapping("/recache/{id}")
  Result recache(@PathVariable Long id);
}
