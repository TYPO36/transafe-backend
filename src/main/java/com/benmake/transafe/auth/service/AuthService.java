package com.benmake.transafe.auth.service;

import com.benmake.transafe.auth.dto.LoginRequest;
import com.benmake.transafe.auth.dto.RegisterRequest;
import com.benmake.transafe.auth.dto.TokenResponse;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.user.entity.UserEntity;
import com.benmake.transafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * 用户登录
     */
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getAccount())
                .or(() -> userRepository.findByPhone(request.getAccount()))
                .orElseThrow(() -> new BusinessException("AUTH_FAILED", "用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("AUTH_FAILED", "密码错误");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_DISABLED", "账户已被禁用");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

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

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 校验邮箱或手机号
        if (request.getEmail() == null && request.getPhone() == null) {
            throw new BusinessException("VALIDATION_ERROR", "邮箱和手机号至少填写一个");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "邮箱已被注册");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("PHONE_EXISTS", "手机号已被注册");
        }

        // 创建用户
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getEmail() != null ? request.getEmail().split("@")[0] : request.getPhone());
        user.setMembershipLevel(0);
        user.setStatus("ACTIVE");

        userRepository.save(user);
        log.info("用户注册成功: email={}, phone={}", request.getEmail(), request.getPhone());
    }

    /**
     * 刷新 Token
     */
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Refresh Token 无效或已过期");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

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
}