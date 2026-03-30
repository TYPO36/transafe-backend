package com.benmake.transafe.infra.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

/**
 * 翻译缓存服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class TranslationCache {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "trans:cache:";
    private static final Duration CACHE_TTL = Duration.ofDays(30);

    /**
     * 生成缓存Key
     */
    private String buildCacheKey(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return CACHE_PREFIX + sb;
        } catch (Exception e) {
            return CACHE_PREFIX + text.hashCode();
        }
    }

    /**
     * 获取翻译缓存
     */
    public Optional<String> get(String text) {
        String key = buildCacheKey(text);
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Optional.of(value.toString());
        }
        return Optional.empty();
    }

    /**
     * 保存翻译缓存
     */
    public void put(String text, String translation) {
        String key = buildCacheKey(text);
        redisTemplate.opsForValue().set(key, translation, CACHE_TTL);
    }

    /**
     * 删除翻译缓存
     */
    public void delete(String text) {
        String key = buildCacheKey(text);
        redisTemplate.delete(key);
    }
}