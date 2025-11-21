package cn.labzen.web.paging.internal;

import cn.labzen.web.paging.Order;
import cn.labzen.web.paging.Pageable;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.*;

import static cn.labzen.web.defination.Constants.DEFAULT_PAGE_NUMBER;
import static cn.labzen.web.defination.Constants.DEFAULT_PAGE_SIZE;

/**
 * 查询请求分页条件解析器，分页条件参数传递规则参考 {@link Pageable}
 */
public final class PageableResolver {

  private PageableResolver() {
  }

  public static Paging resolve(NativeWebRequest webRequest) {
    String parameter = webRequest.getParameter("paging");
    if (parameter != null) {
      return resolveFromCompact(parameter);
    } else {
      return resolveFromNormal(webRequest);
    }
  }

  private static Paging resolveFromCompact(String raw) {
    String[] parts = raw.split(",");

    // 第一部分必须出现，且为正整数，否则默认第一页
    int pageNumber = Optional.ofNullable(parts.length > 0 ? parts[0] : null).flatMap(PageableResolver::parseInt).orElse(DEFAULT_PAGE_NUMBER);

    Optional<String> pageSizeOptional = Optional.ofNullable(parts.length > 1 ? parts[1] : null);
    Integer pageSize = pageSizeOptional.flatMap(PageableResolver::parseInt).orElse(DEFAULT_PAGE_SIZE);

    // 如果第二部分解析不到数据，则认为没有pageSize，orders从第二部分开始解析
    int ordersIndex = pageSizeOptional.isPresent() ? 2 : 1;
    List<String> orderParts = Arrays.asList(parts).subList(ordersIndex, parts.length);
    List<Order> orders = resolveOrders(orderParts);

    // 使用 paging=1,20 这种形式的参数传递，不存在忽略分页需求的情况
    return new Paging(false, pageNumber, pageSize, orders);
  }

  private static Paging resolveFromNormal(NativeWebRequest webRequest) {
    if (webRequest.getParameter("unpaged") != null) {
      return Paging.UNPAGED_PAGING;
    }

    int pageNumber = Optional.ofNullable(webRequest.getParameter("page_number"))
      .or(() -> Optional.ofNullable(webRequest.getParameter("pageNumber")))
      .or(() -> Optional.ofNullable(webRequest.getParameter("pn")))
      .flatMap(PageableResolver::parseInt)
      .orElse(DEFAULT_PAGE_NUMBER);

    int pageSize = Optional.ofNullable(webRequest.getParameter("page_size"))
      .or(() -> Optional.ofNullable(webRequest.getParameter("pageSize")))
      .or(() -> Optional.ofNullable(webRequest.getParameter("ps")))
      .flatMap(PageableResolver::parseInt)
      .orElse(DEFAULT_PAGE_SIZE);

    String ordersRaw = Optional.ofNullable(webRequest.getParameter("orders"))
      .orElse(webRequest.getParameter("od"));

    List<Order> orders = Optional.ofNullable(ordersRaw)
      .map(raw -> List.of(raw.split(",")))
      .filter(s -> !s.isEmpty())
      .map(PageableResolver::resolveOrders)
      .orElse(Collections.emptyList());

    return new Paging(false, pageNumber, pageSize, orders);
  }

  private static List<Order> resolveOrders(List<String> orderParts) {
    List<Order> orders = new ArrayList<>();
    for (String chip : orderParts) {
      String trimmed = chip.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      String name = extractName(trimmed);
      boolean asc = !trimmed.contains("!");
      String nulls = extractNulls(trimmed);

      orders.add(new Order(name, asc, nulls));
    }
    return orders;
  }

  private static String extractName(String trimmed) {
    StringBuilder name = new StringBuilder();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c == '!' || c == '+' || c == '-') {
        break;
      }
      name.append(c);
    }
    return name.toString();
  }

  private static String extractNulls(String trimmed) {
    if (trimmed.endsWith("+")) {
      return "first";
    } else if (trimmed.endsWith("-")) {
      return "last";
    }
    return null;
  }

  private static Optional<Integer> parseInt(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Integer.parseInt(value.trim()));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
