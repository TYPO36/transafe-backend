package com.benmake.transafe.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码重试请求DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequest {

    /**
     * 用户提供的密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
