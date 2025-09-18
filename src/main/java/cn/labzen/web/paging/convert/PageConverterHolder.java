package cn.labzen.web.paging.convert;

import cn.labzen.web.paging.PageConverter;
import lombok.Getter;
import lombok.Setter;

public final class PageConverterHolder {

  @Setter
  @Getter
  private static PageConverter<?> converter;

  private PageConverterHolder() {
  }
}
