package com.benmake.transafe.file.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.file.dto.FileUploadResponse;
import com.benmake.transafe.file.service.FileProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件代理控制器
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Tag(name = "文件", description = "文件上传、下载、列表、删除等操作接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileProxyService fileProxyService;

    @Operation(summary = "上传文件", description = "上传文件到文件存储服务")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        FileUploadResponse result = fileProxyService.uploadFile(file, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "下载文件", description = "根据文件ID下载文件")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        return fileProxyService.downloadFile(fileId, userId);
    }

    @Operation(summary = "获取文件列表", description = "获取当前用户的文件列表")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFiles(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = fileProxyService.listFiles(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "删除文件", description = "根据文件ID删除文件")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        fileProxyService.deleteFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}