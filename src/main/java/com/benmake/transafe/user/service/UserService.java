package com.benmake.transafe.user.service;

import com.benmake.transafe.user.dto.UserInfoResponse;

/**
 * 用户服务接口
 *
 * @author TYPO
 * @since 2026-03-30
 */
public interface UserService {

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户昵称
     *
     * @param userId 用户ID
     * @param nickname 新昵称
     */
    void updateNickname(Long userId, String nickname);

    /**
     * 更新用户头像
     *
     * @param userId 用户ID
     * @param avatar 新头像URL
     */
    void updateAvatar(Long userId, String avatar);
}
