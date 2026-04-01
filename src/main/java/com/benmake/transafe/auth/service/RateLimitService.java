package com.benmake.transafe.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录速率限制服务
 *
 * <p>使用 Redis 实现滑动窗口限流，防止暴力破解攻击。</p>
 *
 * <h3>限流策略</h3>
 * <ul>
 *   <li>最大失败次数：5 次</li>
 *   <li>统计窗口：60 秒</li>
 *   <li>锁定时长：15 分钟</li>
 * </ul>
 *
 * <h3>Redis Key 设计</h3>
 * <ul>
 *   <li>失败计数：auth:fail:{username}</li>
 *   <li>账号锁定：auth:lock:{username}</li>
 * </ul>
 *
 * @author JTP
 * @since 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 最大失败尝试次数
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * 锁定时长（分钟）
     */
    private static final long LOCK_DURATION_MINUTES = 15;

    /**
     * 失败计数窗口时长（秒）
     */
    private static final long WINDOW_DURATION_SECONDS = 60;

    /**
     * 检查账号是否被锁定
     *
     * @param username 用户名
     * @return true-已锁定，false-未锁定
     */
    public boolean isLocked(String username) {
        String lockKey = buildLockKey(username);
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 记录登录失败尝试
     *
     * <p>如果失败次数达到上限，将锁定账号。</p>
     *
     * @param username 用户名
     * @return 当前失败次数
     */
    public long recordFailedAttempt(String username) {
        String failKey = buildFailKey(username);
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1) {
            // 首次失败，设置过期时间
            redisTemplate.expire(failKey, WINDOW_DURATION_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count >= MAX_FAILED_ATTEMPTS) {
            lockAccount(username);
            log.warn("账号已锁定：username={}, 失败次数={}", username, count);
        }

        return count != null ? count : 0;
    }

    /**
     * 锁定账号
     *
     * @param username 用户名
     */
    private void lockAccount(String username) {
        String lockKey = buildLockKey(username);
        redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        // 清除失败计数
        redisTemplate.delete(buildFailKey(username));
        log.info("账号锁定成功：username={}, 锁定时长={}分钟", username, LOCK_DURATION_MINUTES);
    }

    /**
     * 清除失败尝试记录（登录成功后调用）
     *
     * @param username 用户名
     */
    public void clearFailedAttempts(String username) {
        String failKey = buildFailKey(username);
        Boolean deleted = redisTemplate.delete(failKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("清除失败尝试记录：username={}", username);
        }
    }

    /**
     * 获取账号剩余锁定时间
     *
     * @param username 用户名
     * @return 剩余锁定时间（秒），0 表示未锁定
     */
    public long getRemainingLockTime(String username) {
        String lockKey = buildLockKey(username);
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 获取最大失败尝试次数
     *
     * @return 最大失败尝试次数
     */
    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    /**
     * 获取当前失败次数
     *
     * @param username 用户名
     * @return 当前失败次数
     */
    public long getFailedAttempts(String username) {
        String failKey = buildFailKey(username);
        String count = redisTemplate.opsForValue().get(failKey);
        return count != null ? Long.parseLong(count) : 0;
    }

    /**
     * 构建失败计数 Key
     */
    private String buildFailKey(String username) {
        return "auth:fail:" + username;
    }

    /**
     * 构建锁定 Key
     */
    private String buildLockKey(String username) {
        return "auth:lock:" + username;
    }
}