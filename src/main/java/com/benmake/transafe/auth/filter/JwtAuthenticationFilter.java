package com.benmake.transafe.auth.filter;

import com.benmake.transafe.auth.cache.TokenCache;
import com.benmake.transafe.auth.service.JwtService;
import com.benmake.transafe.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenCache tokenCache;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // JWT签名验证
                if (jwtService.isTokenValid(jwt, username)) {
                    Long userId = jwtService.extractUserId(jwt);
                    String jti = jwtService.extractJti(jwt);

                    // 验证Token是否在Redis中存在（实现Token撤销能力）
                    if (!tokenCache.isAccessTokenValid(userId, jti)) {
                        log.warn("Token不在Redis中或已被撤销: userId={}, jti={}", userId, jti);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":" + ErrorCode.TOKEN_REVOKED.getCode() + ",\"message\":\"" + ErrorCode.TOKEN_REVOKED.getMessage() + "\",\"data\":null}");
                        return;
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, Collections.emptyList()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT认证成功: userId={}, jti={}", userId, jti);
                }
            }
        } catch (Exception e) {
            log.warn("JWT 认证失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + ErrorCode.TOKEN_INVALID.getCode() + ",\"message\":\"" + ErrorCode.TOKEN_INVALID.getMessage() + "\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}