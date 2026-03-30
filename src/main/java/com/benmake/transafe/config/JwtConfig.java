package com.benmake.transafe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * Access Token 过期时间（毫秒）
     */
    private Long expiration = 86400000L; // 24小时

    /**
     * Refresh Token 过期时间（毫秒）
     */
    private Long refreshExpiration = 604800000L; // 7天
}