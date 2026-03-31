package com.benmake.transafe.util;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * <p>
 * 提供从 SecurityContext 获取当前登录用户信息的方法
 * </p>
 *
 * @author TYPO
 * @date 2026-03-31
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    /**
     * 获取当前登录用户的ID
     *
     * @return 用户ID
     * @throws BusinessException 如果用户未登录
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "无法获取用户信息");
    }

    /**
     * 获取当前登录用户的ID（可为空）
     *
     * @return 用户ID，如果未登录则返回 null
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true if 已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null;
    }
}
