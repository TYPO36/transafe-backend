package com.benmake.transafe.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 *
 * <p>支持三种登录方式：</p>
 * <ul>
 *   <li>用户名登录</li>
 *   <li>邮箱登录</li>
 *   <li>手机号登录</li>
 * </ul>
 *
 * @author TYPO
 * @since 2026-03-31
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 登录账号
     *
     * <p>可以是用户名、邮箱或手机号</p>
     */
    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号（用户名/邮箱/手机号）", example = "admin")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
