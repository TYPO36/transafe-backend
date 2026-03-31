package com.benmake.transafe.user.service.impl;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.user.dto.UserInfoResponse;
import com.benmake.transafe.user.entity.UserEntity;
import com.benmake.transafe.user.repository.UserRepository;
import com.benmake.transafe.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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

    @Override
    @Transactional
    public void updateNickname(Long userId, String nickname) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setNickname(nickname);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, String avatar) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setAvatar(avatar);
        userRepository.save(user);
    }
}
