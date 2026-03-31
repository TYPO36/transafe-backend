package com.benmake.transafe.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token请求DTO
 *
 * @author JTP
 * @since 2026-03-31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * Refresh Token
     */
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}