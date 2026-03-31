package com.benmake.transafe.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis 工具类
 * <p>
 * 提供常用的 Redis 操作方法封装
 * </p>
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHelper {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 通用操作 ====================

    /**
     * 设置值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值，带过期时间
     */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 获取值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? (T) value : null;
    }

    /**
     * 获取值（返回 Optional）
     */
    public Optional<String> getString(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除键
     */
    public Long delete(Set<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    // ==================== Token 缓存操作 ====================

    private static final String ACCESS_TOKEN_PREFIX = "auth:access:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    /**
     * 存储 Access Token
     */
    public void storeAccessToken(Long userId, String jti, Duration ttl) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, jti, ttl);
        log.debug("存储Access Token: userId={}, jti={}", userId, jti);
    }

    /**
     * 验证 Access Token 是否有效
     */
    public boolean isAccessTokenValid(Long userId, String jti) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        Object storedJti = redisTemplate.opsForValue().get(key);
        return storedJti != null && storedJti.toString().equals(jti);
    }

    /**
     * 删除 Access Token
     */
    public void invalidateAccessToken(Long userId) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("删除Access Token: userId={}", userId);
    }

    /**
     * 存储 Refresh Token
     */
    public void storeRefreshToken(Long userId, String jti, Duration ttl) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, jti, ttl);
        log.debug("存储Refresh Token: userId={}, jti={}", userId, jti);
    }

    /**
     * 获取 Refresh Token JTI
     */
    public String getRefreshTokenJti(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除 Refresh Token
     */
    public void invalidateRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("删除Refresh Token: userId={}", userId);
    }

    /**
     * 删除用户所有 Token（登出）
     */
    public void invalidateAllTokens(Long userId) {
        invalidateAccessToken(userId);
        invalidateRefreshToken(userId);
        log.info("用户登出，清除所有Token: userId={}", userId);
    }

    // ==================== 翻译缓存操作 ====================

    private static final String TRANSLATION_CACHE_PREFIX = "trans:cache:";
    private static final Duration TRANSLATION_CACHE_TTL = Duration.ofDays(30);

    /**
     * 生成翻译缓存Key（MD5摘要）
     */
    public String buildTranslationCacheKey(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return TRANSLATION_CACHE_PREFIX + sb;
        } catch (Exception e) {
            return TRANSLATION_CACHE_PREFIX + text.hashCode();
        }
    }

    /**
     * 获取翻译缓存
     */
    public Optional<String> getTranslation(String text) {
        String key = buildTranslationCacheKey(text);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    /**
     * 保存翻译缓存
     */
    public void putTranslation(String text, String translation) {
        String key = buildTranslationCacheKey(text);
        redisTemplate.opsForValue().set(key, translation, TRANSLATION_CACHE_TTL);
    }

    /**
     * 删除翻译缓存
     */
    public void deleteTranslation(String text) {
        String key = buildTranslationCacheKey(text);
        redisTemplate.delete(key);
    }
}
