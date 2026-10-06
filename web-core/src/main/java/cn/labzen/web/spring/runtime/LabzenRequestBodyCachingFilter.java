package cn.labzen.web.spring.runtime;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求体可重复读过滤器。
 * <p>
 * 将请求包装为 {@link RepeatableReadRequestWrapper}，使请求体可被多次读取，
 * 避免 API 日志读取请求体后，{@code @RequestBody} 因流已被消费而无法绑定。
 */
public class LabzenRequestBodyCachingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                  @Nonnull HttpServletResponse response,
                                  @Nonnull FilterChain filterChain) throws ServletException, IOException {
    filterChain.doFilter(new RepeatableReadRequestWrapper(request), response);
  }
}
