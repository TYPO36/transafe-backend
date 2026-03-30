package com.benmake.transafe.user.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.user.dto.UserInfoResponse;
import com.benmake.transafe.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Tag(name = "用户", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        UserInfoResponse userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @Operation(summary = "更新用户昵称", description = "修改当前用户的昵称")
    @PutMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> request) {
        userService.updateNickname(userId, request.get("nickname"));
        return ResponseEntity.ok(ApiResponse.success("更新成功", null));
    }

    @Operation(summary = "更新用户头像", description = "修改当前用户的头像URL")
    @PutMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> request) {
        userService.updateAvatar(userId, request.get("avatar"));
        return ResponseEntity.ok(ApiResponse.success("更新成功", null));
    }
}