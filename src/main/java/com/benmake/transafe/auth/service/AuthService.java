package com.benmake.transafe.auth.service;

import com.benmake.transafe.auth.dto.LoginRequest;
import com.benmake.transafe.auth.dto.RegisterRequest;
import com.benmake.transafe.auth.dto.TokenResponse;
import org.springframework.security.core.Authentication;

/**
 * 认证服务接口
 *
 * @author TYPO
 * @since 2026-03-31
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>
     * 通过用户名和密码验证用户凭证，生成JWT Token并存储到Redis
     * </p>
     *
     * @param request 登录请求
     * @return Token响应
     */
    TokenResponse login(LoginRequest request);

    /**
     * 用户注册
     * <p>
     * 通过用户名和密码注册，邮箱和手机号可选
     * </p>
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 刷新 Token
     * <p>
     * 验证Refresh Token是否在Redis中有效，生成新的Token
     * </p>
     *
     * @param refreshToken Refresh Token
     * @return 新的Token响应
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     * <p>
     * 清除Redis中用户的所有Token，使Token立即失效
     * </p>
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 用户登出（从Authentication提取userId）
     *
     * @param authentication Spring Security认证对象
     */
    void logout(Authentication authentication);
}
