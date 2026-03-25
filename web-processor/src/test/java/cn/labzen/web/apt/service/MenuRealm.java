package cn.labzen.web.apt.service;

import cn.labzen.web.api.definition.FileFormat;
import cn.labzen.web.api.definition.UploadedFile;
import cn.labzen.web.api.response.result.Result;
import cn.labzen.web.api.response.result.Results;
import cn.labzen.web.api.service.FileHandleService;
import cn.labzen.web.api.service.StandardResourceService;
import com.google.common.collect.Lists;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 菜单域
 */
@Service
public class MenuRealm implements StandardResourceService<MenuDto, Long>, FileHandleService<MenuDto> {

  @Nonnull
  @Override
  public Result create(@Nonnull MenuDto resource) {
    return Results.status(HttpStatus.NOT_ACCEPTABLE);
  }

  @Nonnull
  @Override
  public Result edit(@Nonnull Long id, @Nonnull MenuDto resource) {
    return Results.status(HttpStatus.NOT_ACCEPTABLE);
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
    return Results.status(HttpStatus.NOT_ACCEPTABLE);
  }

  public Result recache(Long id) {
    return null;
  }

  @Override
  public Result exports(MenuDto resource, FileFormat format) {
    return null;
  }

  @Override
  public Result imports(UploadedFile uploadedFile) {
    return null;
  }
}
