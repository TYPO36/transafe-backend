package com.benmake.transafe.auth.service.impl;

import com.benmake.transafe.auth.cache.TokenCache;
import com.benmake.transafe.auth.dto.LoginRequest;
import com.benmake.transafe.auth.dto.RegisterRequest;
import com.benmake.transafe.auth.dto.TokenResponse;
import com.benmake.transafe.auth.service.AuthService;
import com.benmake.transafe.auth.service.JwtService;
import com.benmake.transafe.auth.service.RateLimitService;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.infra.mapper.UserMapper;
import com.benmake.transafe.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenCache tokenCache;
    private final RateLimitService rateLimitService;

    @Override
    public TokenResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 1. 检查账号是否被锁定
        if (rateLimitService.isLocked(username)) {
            long remainingTime = rateLimitService.getRemainingLockTime(username);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                "账号已锁定，请 " + remainingTime + " 秒后重试");
        }

        // 2. 查询用户
        UserEntity user = userMapper.findByUsername(username)
                .orElseThrow(() -> {
                    rateLimitService.recordFailedAttempt(username);
                    return new BusinessException(ErrorCode.LOGIN_FAILED);
                });

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            long failCount = rateLimitService.recordFailedAttempt(username);
            int remaining = rateLimitService.getMaxFailedAttempts() - (int) failCount;
            if (remaining > 0) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED,
                    "密码错误，剩余尝试次数：" + remaining);
            } else {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "失败次数过多，账号已被锁定15分钟");
            }
        }

        // 4. 检查账户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 5. 登录成功，清除失败记录
        rateLimitService.clearFailedAttempts(username);

        // 6. 生成Token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 7. 存储Token到Redis
        String accessJti = jwtService.extractJti(accessToken);
        String refreshJti = jwtService.extractJti(refreshToken);
        tokenCache.storeAccessToken(user.getId(), accessJti);
        tokenCache.storeRefreshToken(user.getId(), refreshJti);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(7200L)
                .userInfo(TokenResponse.UserInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .membershipLevel(user.getMembershipLevel())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 校验用户名唯一性
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验邮箱唯一性（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        // 校验手机号唯一性（如果提供了手机号）
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userMapper.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        // 创建用户（时间字段由MyBatis Plus自动填充）
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getUsername());
        user.setMembershipLevel(0);
        user.setStatus("ACTIVE");

        userMapper.insert(user);
        log.info("用户注册成功: username={}, email={}, phone={}",
                request.getUsername(), request.getEmail(), request.getPhone());
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh Token 无效或已过期");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        String currentJti = jwtService.extractJti(refreshToken);

        // 验证Refresh Token是否在Redis中存在
        String storedJti = tokenCache.getRefreshTokenJti(userId);
        if (storedJti == null || !storedJti.equals(currentJti)) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 生成新Token
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        // 更新Redis中的Token
        String newAccessJti = jwtService.extractJti(newAccessToken);
        String newRefreshJti = jwtService.extractJti(newRefreshToken);
        tokenCache.invalidateAllTokens(userId);
        tokenCache.storeAccessToken(user.getId(), newAccessJti);
        tokenCache.storeRefreshToken(user.getId(), newRefreshJti);

        log.info("Token刷新成功: userId={}", userId);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(7200L)
                .userInfo(TokenResponse.UserInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .membershipLevel(user.getMembershipLevel())
                        .build())
                .build();
    }

    @Override
    public void logout(Long userId) {
        tokenCache.invalidateAllTokens(userId);
        log.info("用户登出成功: userId={}", userId);
    }

    @Override
    public void logout(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = (Long) authentication.getPrincipal();
        logout(userId);
    }
}