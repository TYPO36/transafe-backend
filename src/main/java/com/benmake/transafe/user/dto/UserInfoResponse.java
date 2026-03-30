package com.benmake.transafe.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户信息响应
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer membershipLevel;
    private BigDecimal balance;
    private String status;
}