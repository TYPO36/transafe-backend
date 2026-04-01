package com.benmake.transafe.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 超级管理员 API 密钥认证过滤器
 *
 * <p>当请求携带 Authorization: 123 时，识别为超级管理员。</p>
 *
 * <h3>安全警告</h3>
 * <p>此机制仅适用于开发环境或内部管理系统，生产环境请使用更安全的认证方式。</p>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    /**
     * 超级管理员 API 密钥
     *
     * <p>生产环境应通过环境变量 ADMIN_API_KEY 配置。</p>
     */
    private static final String ADMIN_API_KEY = "123";

    /**
     * 超级管理员用户 ID
     */
    private static final Long ADMIN_USER_ID = -1L;

    /**
     * 超级管理员用户名
     */
    private static final String ADMIN_USERNAME = "SUPER_ADMIN";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 如果已有认证信息，跳过
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // 检查是否为管理员 API 密钥
        if (authHeader != null && authHeader.equals(ADMIN_API_KEY)) {
            // 创建超级管理员认证信息
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            ADMIN_USER_ID,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("超级管理员认证成功: uri={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}