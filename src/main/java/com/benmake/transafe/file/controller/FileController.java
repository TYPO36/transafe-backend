package com.benmake.transafe.file.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.file.dto.FileInfoResponse;
import com.benmake.transafe.file.service.FileProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文件控制器
 * <p>
 * 只负责文件存储的增删改查，不涉及文档解析
 * </p>
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Tag(name = "文件", description = "文件下载、列表、删除等操作接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileProxyService fileProxyService;

    @Operation(summary = "下载文件", description = "根据文件ID下载文件")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        Resource resource = fileProxyService.downloadFile(fileId, userId);
        String fileName = fileProxyService.getFileName(fileId, userId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @Operation(summary = "获取文件信息", description = "根据文件ID获取文件信息")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getFileInfo(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        FileInfoResponse result = fileProxyService.getFileInfo(fileId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
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

    @Operation(summary = "更新文件信息", description = "更新文件名等信息")
    @PutMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileInfoResponse>> updateFileInfo(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(description = "新文件名") @RequestParam("fileName") String fileName,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        FileInfoResponse result = fileProxyService.updateFileInfo(fileId, userId, fileName);
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
