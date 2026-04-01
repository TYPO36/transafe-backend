package com.benmake.transafe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * JWT 密钥安全校验器
 *
 * <p>在应用启动后校验 JWT_SECRET 是否满足安全要求：</p>
 * <ul>
 *   <li>密钥长度至少 32 字节（HS256 要求 256 位）</li>
 *   <li>不能使用不安全的默认值</li>
 * </ul>
 *
 * <h3>配置方式</h3>
 * <p>生产环境必须通过环境变量设置：</p>
 * <pre>
 * export JWT_SECRET="your-secure-random-secret-key-at-least-32-bytes"
 * </pre>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Component
public class JwtSecretValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 最小密钥长度（HS256 算法要求 256 位 = 32 字节）
     */
    private static final int MIN_SECRET_LENGTH = 32;

    /**
     * 不安全的默认值关键词
     */
    private static final String[] UNSAFE_DEFAULTS = {
        "your-secret-key",
        "must-be-at-least",
        "default",
        "test",
        "example"
    };

    /**
     * 开发环境默认密钥前缀（允许启动但输出警告）
     */
    private static final String DEV_DEFAULT_PREFIX = "Transafe2026DevSecretKey";

    /**
     * 应用启动后校验 JWT 密钥
     *
     * <p>仅输出警告，不阻止应用启动。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecret() {
        // 优先检查环境变量
        String envSecret = System.getenv("JWT_SECRET");
        String actualSecret = envSecret != null && !envSecret.isBlank() ? envSecret : jwtSecret;

        log.info("JWT 密钥校验开始...");

        // 校验长度
        if (actualSecret == null || actualSecret.length() < MIN_SECRET_LENGTH) {
            log.warn("========================================");
            log.warn("警告：JWT_SECRET 密钥长度不足！当前 {} 字节，建议至少 {} 字节",
                    actualSecret == null ? 0 : actualSecret.length(), MIN_SECRET_LENGTH);
            log.warn("========================================");
            return;
        }

        // 检查是否使用开发环境默认密钥
        if (actualSecret.startsWith(DEV_DEFAULT_PREFIX)) {
            log.warn("========================================");
            log.warn("警告：正在使用开发环境默认 JWT 密钥！");
            log.warn("生产环境必须设置环境变量 JWT_SECRET");
            log.warn("========================================");
            return;
        }

        // 校验是否使用不安全的默认值
        for (String unsafe : UNSAFE_DEFAULTS) {
            if (actualSecret.toLowerCase().contains(unsafe)) {
                log.warn("========================================");
                log.warn("警告：JWT_SECRET 使用了不安全的默认值！检测到关键词 '{}'", unsafe);
                log.warn("生产环境必须设置自定义密钥");
                log.warn("========================================");
                return;
            }
        }

        // 校验通过
        if (envSecret != null && !envSecret.isBlank()) {
            log.info("JWT 密钥校验通过，使用环境变量配置，密钥长度：{} 字节", actualSecret.length());
        } else {
            log.info("JWT 密钥校验通过，使用配置文件值，密钥长度：{} 字节", actualSecret.length());
        }
    }
}