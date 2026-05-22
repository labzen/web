package cn.labzen.web.paging.convert;

import cn.labzen.web.api.paging.PageConverter;

import java.util.concurrent.atomic.AtomicReference;

public final class PageConverterHolder {

  private static final AtomicReference<PageConverter<?>> CONVERTER_REFERENCE = new AtomicReference<>(null);

  public static void setConverter(PageConverter<?> converter) {
    CONVERTER_REFERENCE.set(converter);
  }

  public static PageConverter<?> getConverter() {
    return CONVERTER_REFERENCE.get();
  }

  private PageConverterHolder() {
  }
}
