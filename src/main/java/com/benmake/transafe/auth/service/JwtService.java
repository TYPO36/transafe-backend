package com.benmake.transafe.auth.service;

import com.benmake.transafe.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT 服务
 *
 * @author TYPO
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token
     * <p>
     * 包含用户ID、用户名和唯一JWT标识(JTI)
     * </p>
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateAccessToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, username, jwtConfig.getExpiration());
    }

    /**
     * 生成 Refresh Token
     * <p>
     * 包含用户ID和唯一JWT标识(JTI)
     * </p>
     *
     * @param userId 用户ID
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, String.valueOf(userId), jwtConfig.getRefreshExpiration());
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 提取用户名（subject）
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 Token 提取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * 从 Token 提取JWT唯一标识(JTI)
     *
     * @param token JWT Token
     * @return JTI
     */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    /**
     * 提取指定声明
     *
     * @param token         JWT Token
     * @param claimsResolver 声明解析器
     * @return 声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token    JWT Token
     * @param username 用户名
     * @return 是否有效
     */
    public boolean isTokenValid(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Refresh Token 是否有效
     *
     * @param token Refresh Token
     * @return 是否有效
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Refresh Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
