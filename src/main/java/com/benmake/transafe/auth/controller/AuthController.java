package com.benmake.transafe.auth.controller;

import com.benmake.transafe.auth.dto.LoginRequest;
import com.benmake.transafe.auth.dto.RegisterRequest;
import com.benmake.transafe.auth.dto.TokenResponse;
import com.benmake.transafe.auth.service.AuthService;
import com.benmake.transafe.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Tag(name = "认证", description = "用户登录、注册、Token刷新等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "通过用户名和密码登录，获取JWT Token")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "用户注册", description = "注册新用户账号")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", null));
    }

    @Operation(summary = "刷新Token", description = "使用refreshToken获取新的访问Token")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        TokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}