package com.benmake.transafe.user.service;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.user.dto.UserInfoResponse;
import com.benmake.transafe.user.entity.UserEntity;
import com.benmake.transafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 获取用户信息
     */
    public UserInfoResponse getUserInfo(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));

        return UserInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .membershipLevel(user.getMembershipLevel())
                .balance(user.getBalance())
                .status(user.getStatus())
                .build();
    }

    /**
     * 更新用户昵称
     */
    @Transactional
    public void updateNickname(Long userId, String nickname) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        user.setNickname(nickname);
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateAvatar(Long userId, String avatar) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        user.setAvatar(avatar);
    }
}