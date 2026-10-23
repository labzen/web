package cn.labzen.web.paging.internal;

import cn.labzen.meta.Labzens;
import cn.labzen.web.api.paging.Order;
import cn.labzen.web.meta.WebCoreConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

import java.io.IOException;
import java.util.*;

import static cn.labzen.web.api.definition.Constants.DEFAULT_PAGE_NUMBER;
import static cn.labzen.web.paging.internal.Paging.DEFAULT_PAGE_SIZE;

/**
 * 分页条件解析器
 * <p>
 * 将 HTTP 请求中的分页条件解析为分页数据，条件来源按优先级依次为：
 * <ol>
 *   <li>query string 的紧凑格式：{@code paging=1,20,field1,-field2}</li>
 *   <li>query string 的普通格式：{@code pageNumber=1&pageSize=20&orders=field1,!field2}</li>
 *   <li>JSON 请求体中同名的分页字段（由 {@code LabzenPageableRequestBodyAdvice} 解析后放入请求属性）</li>
 * </ol>
 * 三者都没有时使用默认分页（第 1 页、每页 {@link Paging#DEFAULT_PAGE_SIZE} 条）。
 */
public final class PageableResolver {

  /**
   * 请求属性名：JSON 请求体中解析出的分页条件
   */
  public static final String PAGING_ATTRIBUTE = "cn.labzen.web.paging.body";

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int MAX_PAGE_SIZE;

  static {
    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
    MAX_PAGE_SIZE = configuration.maxPageSize();
  }

  private PageableResolver() {
  }

  /**
   * 解析分页条件
   *
   * @param webRequest Web 请求
   * @return 解析后的分页条件
   */
  public static Paging resolve(NativeWebRequest webRequest) {
    String parameter = webRequest.getParameter("paging");
    if (parameter != null) {
      return resolveFromCompact(parameter);
    }

    Paging fromQuery = resolveFromNormal(webRequest);
    if (fromQuery != null) {
      return fromQuery;
    }

    Paging fromBody = resolveFromAttribute(webRequest);
    if (fromBody != null) {
      return fromBody;
    }

    return Paging.DEFAULT_PAGING;
  }

  /**
   * 从紧凑格式解析分页条件
   * <p>
   * 格式：page,size,field1,-field2,nulls+
   * - 第一部分：页码（必填）
   * - 第二部分：每页数量（可选）
   * - 第三部分及之后：排序字段（可选，!开头表示降序，+结尾表示nulls first，-结尾表示nulls last）
   */
  private static Paging resolveFromCompact(String raw) {
    String[] parts = raw.split(",");

    // 第一部分必须出现，且为正整数，否则默认第一页
    int pageNumber = Optional.ofNullable(parts.length > 0 ? parts[0] : null)
                             .flatMap(PageableResolver::parseInt)
                             .orElse(DEFAULT_PAGE_NUMBER);

    Optional<String> pageSizeOptional = Optional.ofNullable(parts.length > 1 ? parts[1] : null);
    int pageSize = pageSizeOptional.flatMap(PageableResolver::parseInt).orElse(DEFAULT_PAGE_SIZE);
    if (pageSize > MAX_PAGE_SIZE) {
      pageSize = MAX_PAGE_SIZE;
    }

    // 如果第二部分解析不到数据，则认为没有pageSize，orders从第二部分开始解析
    int ordersIndex = pageSizeOptional.isPresent() ? 2 : 1;
    List<String> orderParts = Arrays.asList(parts).subList(ordersIndex, parts.length);
    List<Order> orders = resolveOrders(orderParts);

    // 使用 paging=1,20 这种形式的参数传递，不存在忽略分页需求的情况
    return new Paging(false, pageNumber, pageSize, orders);
  }

  /**
   * 从普通格式的 query string 解析分页条件
   * <p>
   * 支持的参数名：
   * - 页码：page_number, pageNumber, pn
   * - 每页数量：page_size, pageSize, ps
   * - 排序：orders, od
   * - 禁用分页：unpaged
   *
   * @return 解析后的分页条件；query string 未提供任何分页条件时返回 {@code null}
   */
  private static Paging resolveFromNormal(NativeWebRequest webRequest) {
    if (webRequest.getParameter("unpaged") != null) {
      return Paging.UNPAGED_PAGING;
    }

    Integer pageNumber = firstInt(webRequest, "page_number", "pageNumber", "pn");
    Integer pageSize = firstInt(webRequest, "page_size", "pageSize", "ps");
    String ordersRaw = Optional.ofNullable(webRequest.getParameter("orders"))
                               .orElse(webRequest.getParameter("od"));

    if (pageNumber == null && pageSize == null && ordersRaw == null) {
      return null;
    }

    return new Paging(false,
        Optional.ofNullable(pageNumber).orElse(DEFAULT_PAGE_NUMBER),
        limit(Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE)),
        Optional.ofNullable(ordersRaw)
                .map(raw -> List.of(raw.split(",")))
                .filter(s -> !s.isEmpty())
                .map(PageableResolver::resolveOrders)
                .orElse(Collections.emptyList()));
  }

  /**
   * 解析 JSON 请求体中的分页条件
   * <p>
   * 支持的字段名与 query string 一致：{@code unpaged}、{@code pageNumber}/{@code page_number}/{@code pn}、
   * {@code pageSize}/{@code page_size}/{@code ps}、{@code orders}/{@code od}。
   * 字段值可以是数字，也可以是数字字符串；{@code orders} 可以是数组或逗号分隔的字符串。
   *
   * @param body 请求体字节
   * @return 解析后的分页条件；请求体不是 JSON 对象、或未包含任何分页字段时返回 {@code null}
   */
  public static Paging resolveFromBody(byte[] body) {
    if (body == null || body.length == 0) {
      return null;
    }

    JsonNode root;
    try {
      root = JSON.readTree(body);
    } catch (IOException e) {
      return null;
    }
    if (root == null || !root.isObject()) {
      return null;
    }

    JsonNode unpagedNode = root.get("unpaged");
    if (unpagedNode != null && !unpagedNode.isNull() && unpagedNode.asBoolean(false)) {
      return Paging.UNPAGED_PAGING;
    }

    Integer pageNumber = jsonInt(root, "pageNumber", "page_number", "pn");
    Integer pageSize = jsonInt(root, "pageSize", "page_size", "ps");
    List<Order> orders = jsonOrders(root);

    if (pageNumber == null && pageSize == null && orders == null) {
      return null;
    }

    return new Paging(false,
        Optional.ofNullable(pageNumber).orElse(DEFAULT_PAGE_NUMBER),
        limit(Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE)),
        Optional.ofNullable(orders).orElse(Collections.emptyList()));
  }

  private static Paging resolveFromAttribute(NativeWebRequest webRequest) {
    Object attribute = webRequest.getAttribute(PAGING_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    return attribute instanceof Paging paging ? paging : null;
  }

  private static Integer firstInt(NativeWebRequest webRequest, String... names) {
    for (String name : names) {
      Optional<Integer> parsed = parseInt(webRequest.getParameter(name));
      if (parsed.isPresent()) {
        return parsed.get();
      }
    }
    return null;
  }

  private static Integer jsonInt(JsonNode root, String... names) {
    for (String name : names) {
      JsonNode node = root.get(name);
      if (node == null || node.isNull()) {
        continue;
      }
      Optional<Integer> parsed = node.isNumber() ? Optional.of(node.asInt()) : parseInt(node.asText());
      if (parsed.isPresent()) {
        return parsed.get();
      }
    }
    return null;
  }

  private static List<Order> jsonOrders(JsonNode root) {
    JsonNode node = root.get("orders");
    if (node == null || node.isNull()) {
      node = root.get("od");
    }
    if (node == null || node.isNull()) {
      return null;
    }

    List<String> chips = new ArrayList<>();
    if (node.isArray()) {
      node.forEach(item -> chips.add(item.asText()));
    } else {
      chips.addAll(Arrays.asList(node.asText().split(",")));
    }
    return resolveOrders(chips);
  }

  private static int limit(int pageSize) {
    return Math.min(pageSize, MAX_PAGE_SIZE);
  }

  /**
   * 解析排序字段列表
   */
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

  /**
   * 从排序字符串中提取字段名
   * <p>
   * 排序修饰符：! 降序，+ nulls first，- nulls last
   */
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

  /**
   * 从排序字符串中提取 nulls 处理方式
   */
  private static String extractNulls(String trimmed) {
    if (trimmed.endsWith("+")) {
      return "first";
    } else if (trimmed.endsWith("-")) {
      return "last";
    }
    return null;
  }

  /**
   * 安全解析整数
   * <p>
   * 如果字符串为空或格式不正确，返回空 Optional。
   */
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
