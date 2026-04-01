package com.benmake.transafe.auth.service.impl;

import com.benmake.transafe.auth.cache.TokenCache;
import com.benmake.transafe.auth.dto.LoginRequest;
import com.benmake.transafe.auth.dto.RegisterRequest;
import com.benmake.transafe.auth.dto.TokenResponse;
import com.benmake.transafe.auth.service.AuthService;
import com.benmake.transafe.auth.service.JwtService;
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

import java.time.LocalDateTime;

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

    @Override
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 生成Token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 存储Token到Redis
        String accessJti = jwtService.extractJti(accessToken);
        String refreshJti = jwtService.extractJti(refreshToken);
        tokenCache.storeAccessToken(user.getId(), accessJti);
        tokenCache.storeRefreshToken(user.getId(), refreshJti);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400L)
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

        // 创建用户
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getUsername());
        user.setMembershipLevel(0);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

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
                .expiresIn(86400L)
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