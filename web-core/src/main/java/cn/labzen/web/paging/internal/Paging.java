package cn.labzen.web.paging.internal;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.paging.Order;
import cn.labzen.web.api.paging.Pageable;
import cn.labzen.web.meta.WebConfiguration;

import java.util.Collections;
import java.util.List;

import static cn.labzen.web.api.definition.Constants.DEFAULT_PAGE_NUMBER;

/**
 * 存储分页条件数据，用于{@link Pageable}数据代理提供方
 */

public record Paging(boolean unpaged, int pageNumber, int pageSize, List<Order> orders) implements Pageable {

  public static final int DEFAULT_PAGE_SIZE = Labzens.configurationWith(WebConfiguration.class).pageSize();
  public static final Paging DEFAULT_PAGING = new Paging(false, 0, 0, Collections.emptyList());
  public static final Paging UNPAGED_PAGING = new Paging(true, 0, 0, Collections.emptyList());

  public Paging(boolean unpaged, int pageNumber, int pageSize, List<Order> orders) {
    this.unpaged = unpaged;
    this.pageNumber = pageNumber <= 0 ? DEFAULT_PAGE_NUMBER : pageNumber;
    this.pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
    this.orders = orders;
  }

  @Override
  public <T> T convertTo() {
    throw new UnsupportedOperationException("not invoke this convert method");
  }

}
