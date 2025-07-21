package cn.labzen.web.ap.service;

import cn.labzen.web.response.bean.Result;
import cn.labzen.web.response.bean.Results;
import cn.labzen.web.service.StandardResourceService;
import com.google.common.collect.Lists;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 菜单域
 */
@Service
public class MenuRealm implements StandardResourceService<MenuDto, Long> {

  @Nonnull
  @Override
  public Result create(@Nonnull MenuDto resource) {
    return Results.justStatus(HttpStatus.NOT_ACCEPTABLE);
  }

  @Nonnull
  @Override
  public Result edit(@Nonnull Long id, @Nonnull MenuDto resource) {
    return Results.justStatus(HttpStatus.NOT_ACCEPTABLE);
  }

  @Nonnull
  @Override
  public Result info(@Nonnull Long id) {
    return Results.with(new MenuDto());
  }

  @Nonnull
  @Override
  public Result find(@Nonnull MenuDto resource) {
    // 使用code来指定 ROOT CODE
    List<MenuDto> roots = Lists.newArrayList();
    return Results.with(roots);
  }

  @Nonnull
  @Override
  public Result remove(@Nonnull Long id) {
    return Results.justStatus(HttpStatus.NOT_ACCEPTABLE);
  }

  public Result recache(Long id) {
    return null;
  }
}
