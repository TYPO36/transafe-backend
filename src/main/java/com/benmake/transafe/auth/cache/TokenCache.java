package com.benmake.transafe.auth.cache;

import com.benmake.transafe.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token缓存服务
 * <p>
 * 负责将Token存储到Redis，实现Token撤销和会话管理
 * </p>
 *
 * @author JTP
 * @since 2026-03-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    /**
     * Access Token Key前缀
     * 格式: auth:access:{userId}
     * <p>
     * 每个用户只保留一个有效的Access Token，实现单设备登录
     * </p>
     */
    private static final String ACCESS_PREFIX = "auth:access:";

    /**
     * Refresh Token Key前缀
     * 格式: auth:refresh:{userId}
     */
    private static final String REFRESH_PREFIX = "auth:refresh:";

    /**
     * 存储Access Token JTI到Redis
     * <p>
     * 每个用户只保留一个有效的Access Token，实现单设备登录。
     * 新登录会覆盖旧的Token，使旧Token立即失效。
     * TTL与JWT配置的过期时间同步。
     * </p>
     *
     * @param userId 用户ID
     * @param jti    JWT唯一标识
     */
    public void storeAccessToken(Long userId, String jti) {
        String key = buildAccessKey(userId);
        Duration ttl = Duration.ofMillis(jwtConfig.getExpiration());
        redisTemplate.opsForValue().set(key, jti, ttl);
        log.debug("存储Access Token: userId={}, jti={}, ttl={}ms", userId, jti, jwtConfig.getExpiration());
    }

    /**
     * 验证Access Token是否在Redis中有效
     * <p>
     * 检查Redis中存储的JTI与请求中的JTI是否匹配
     * </p>
     *
     * @param userId 用户ID
     * @param jti    JWT唯一标识
     * @return true-有效，false-已失效或不存在
     */
    public boolean isAccessTokenValid(Long userId, String jti) {
        String key = buildAccessKey(userId);
        Object storedJti = redisTemplate.opsForValue().get(key);
        if (storedJti == null) {
            return false;
        }
        return storedJti.toString().equals(jti);
    }

    /**
     * 存储Refresh Token JTI到Redis
     * <p>
     * 每个用户只保留一个有效的Refresh Token，实现单设备登录。
     * TTL与JWT配置的刷新Token过期时间同步。
     * </p>
     *
     * @param userId 用户ID
     * @param jti    JWT唯一标识
     */
    public void storeRefreshToken(Long userId, String jti) {
        String key = buildRefreshKey(userId);
        Duration ttl = Duration.ofMillis(jwtConfig.getRefreshExpiration());
        redisTemplate.opsForValue().set(key, jti, ttl);
        log.debug("存储Refresh Token: userId={}, jti={}, ttl={}ms", userId, jti, jwtConfig.getRefreshExpiration());
    }

    /**
     * 获取用户的Refresh Token JTI
     *
     * @param userId 用户ID
     * @return JTI字符串，不存在则返回null
     */
    public String getRefreshTokenJti(Long userId) {
        String key = buildRefreshKey(userId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除用户的Access Token
     *
     * @param userId 用户ID
     */
    public void invalidateAccessToken(Long userId) {
        String key = buildAccessKey(userId);
        redisTemplate.delete(key);
        log.debug("删除Access Token: userId={}", userId);
    }

    /**
     * 删除用户的Refresh Token
     *
     * @param userId 用户ID
     */
    public void invalidateRefreshToken(Long userId) {
        String key = buildRefreshKey(userId);
        redisTemplate.delete(key);
        log.debug("删除Refresh Token: userId={}", userId);
    }

    /**
     * 删除用户所有Token（登出）
     * <p>
     * 清除Access Token和Refresh Token
     * </p>
     *
     * @param userId 用户ID
     */
    public void invalidateAllTokens(Long userId) {
        invalidateAccessToken(userId);
        invalidateRefreshToken(userId);
        log.info("用户登出，清除所有Token: userId={}", userId);
    }

    /**
     * 构建Access Token Key
     */
    private String buildAccessKey(Long userId) {
        return ACCESS_PREFIX + userId;
    }

    /**
     * 构建Refresh Token Key
     */
    private String buildRefreshKey(Long userId) {
        return REFRESH_PREFIX + userId;
    }
}