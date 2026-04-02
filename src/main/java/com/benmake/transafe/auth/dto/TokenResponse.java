package com.benmake.transafe.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 响应
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserInfo userInfo;

    /**
     * 用户信息
     *
     * <p>登录成功后返回的用户基本信息，供前端展示使用</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 头像URL
         */
        private String avatar;

        /**
         * 用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员
         */
        private String role;

        /**
         * 会员等级：0-普通，1-VIP
         */
        private Integer membershipLevel;
    }
}