package com.benmake.transafe.quota.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.quota.dto.QuotaStatusResponse;
import com.benmake.transafe.quota.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配额控制器
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Tag(name = "配额", description = "用户配额状态查询接口")
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    @Operation(summary = "获取配额状态", description = "获取当前用户的配额使用情况（剩余字符数、文件数等）")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QuotaStatusResponse>> getQuotaStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        QuotaStatusResponse status = quotaService.getQuotaStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}