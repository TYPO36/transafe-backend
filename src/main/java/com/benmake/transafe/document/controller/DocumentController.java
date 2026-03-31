package com.benmake.transafe.document.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.document.dto.*;
import com.benmake.transafe.document.service.DocumentService;
import com.benmake.transafe.document.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档控制器
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Slf4j
@Tag(name = "文档", description = "文档解析、上传、查询、搜索等接口")
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final SearchService searchService;

    /**
     * 上传文件
     */
    @Operation(summary = "上传文件", description = "上传单个文件并进行解析")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        DocumentDTO result = documentService.uploadAndCreateDocument(file, userId, false);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 批量上传文件
     */
    @Operation(summary = "批量上传文件", description = "批量上传多个文件，建议≤100个")
    @PostMapping("/upload/batch")
    public ResponseEntity<ApiResponse<BatchUploadResponse>> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        BatchUploadResponse response = documentService.batchUploadAndCreateDocument(files, userId, false);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 查询解析状态
     */
    @Operation(summary = "查询解析状态", description = "查询指定文档的解析状态")
    @GetMapping("/{fileId}/status")
    public ResponseEntity<ApiResponse<DocumentDTO>> getStatus(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        DocumentDTO result = documentService.getDocumentStatus(fileId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取文档树
     */
    @Operation(summary = "获取文档树", description = "获取文档及其附件的树形结构")
    @GetMapping("/{fileId}/tree")
    public ResponseEntity<ApiResponse<DocumentTreeDTO>> getTree(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        DocumentTreeDTO result = documentService.getDocumentTree(fileId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 密码重试解析
     */
    @Operation(summary = "密码重试解析", description = "对密码保护文件使用密码重试解析")
    @PostMapping("/{fileId}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Valid @RequestBody RetryRequest request) {

        documentService.retryWithPassword(fileId, request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("解析任务已提交", null));
    }

    /**
     * 全文搜索
     */
    @Operation(summary = "全文搜索", description = "搜索文档内容，支持关键词高亮")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResultDTO>> search(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        SearchResultDTO result = searchService.search(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取文档详情
     */
    @Operation(summary = "获取文档详情", description = "获取文档的完整信息")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<DocumentDTO>> getDocument(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        DocumentDTO result = documentService.getDocument(fileId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取解析进度
     */
    @Operation(summary = "获取解析进度", description = "根据根文档ID查询解析进度")
    @GetMapping("/progress/{rootId}")
    public ResponseEntity<ApiResponse<ParseProgressDTO>> getParseProgress(
            @Parameter(description = "根文档ID") @PathVariable String rootId) {

        ParseProgressDTO result = documentService.getParseProgress(rootId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
