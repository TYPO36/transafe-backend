package com.benmake.transafe.file.client;

import com.benmake.transafe.file.dto.FileUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传服务 Feign 客户端
 *
 * @author TYPO
 * @since 2026-03-30
 */
@FeignClient(name = "file-storage", url = "${file-storage.url:http://localhost:8081}")
public interface FileUploadClient {

    /**
     * 上传文件
     */
    @PostMapping(value = "/api/internal/files/upload", consumes = "multipart/form-data")
    FileUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("userId") Long userId
    );

    /**
     * 下载文件
     */
    @GetMapping("/api/internal/files/{fileId}/download")
    ResponseEntity<Resource> downloadFile(@PathVariable("fileId") String fileId);

    /**
     * 获取文件信息
     */
    @GetMapping("/api/internal/files/{fileId}/info")
    Map<String, Object> getFileInfo(@PathVariable("fileId") String fileId);

    /**
     * 获取文件列表
     */
    @GetMapping("/api/internal/files")
    Map<String, Object> listFiles(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    );

    /**
     * 删除文件
     */
    @DeleteMapping("/api/internal/files/{fileId}")
    Map<String, Object> deleteFile(@PathVariable("fileId") String fileId);
}