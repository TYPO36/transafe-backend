package com.benmake.transafe.file.client;

import com.benmake.transafe.file.dto.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock文件存储客户端
 * 用于开发和测试环境，替代外部文件存储服务
 *
 * @author JTP
 * @date 2026-03-31
 */
@Slf4j
@Component
public class MockFileUploadClient {

    // 模拟文件存储
    private static final Map<String, Map<String, Object>> FILE_STORE = new ConcurrentHashMap<>();

    /**
     * 上传文件
     */
    public FileUploadResponse uploadFile(MultipartFile file, Long userId) {
        String fileId = "file-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            byte[] content = file.getBytes();

            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("fileId", fileId);
            fileInfo.put("fileName", file.getOriginalFilename());
            fileInfo.put("fileSize", content.length);
            fileInfo.put("fileType", getFileExtension(file.getOriginalFilename()));
            fileInfo.put("userId", userId);
            fileInfo.put("status", "UPLOADED");
            fileInfo.put("createdAt", Instant.now().toString());
            fileInfo.put("content", Base64.getEncoder().encodeToString(content));

            FILE_STORE.put(fileId, fileInfo);

            log.info("Mock上传文件成功: fileId={}, fileName={}, size={}", fileId, file.getOriginalFilename(), content.length);

            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(fileId);
            response.setFileName(file.getOriginalFilename());
            response.setFileSize((long) content.length);
            response.setFileType(getFileExtension(file.getOriginalFilename()));

            return response;
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }

    /**
     * 下载文件
     */
    public ResponseEntity<Resource> downloadFile(String fileId) {
        Map<String, Object> fileInfo = FILE_STORE.get(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        String contentBase64 = (String) fileInfo.get("content");
        byte[] content = Base64.getDecoder().decode(contentBase64);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileInfo.get("fileName") + "\"")
                .body(new ByteArrayResource(content));
    }

    /**
     * 获取文件信息
     */
    public Map<String, Object> getFileInfo(String fileId) {
        Map<String, Object> fileInfo = FILE_STORE.get(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        Map<String, Object> result = new HashMap<>(fileInfo);
        result.remove("content"); // 不返回文件内容
        return result;
    }

    /**
     * 获取文件列表
     */
    public Map<String, Object> listFiles(Long userId, int page, int size) {
        List<Map<String, Object>> userFiles = FILE_STORE.values().stream()
                .filter(f -> userId.equals(f.get("userId")))
                .sorted((a, b) -> ((String) b.get("createdAt")).compareTo((String) a.get("createdAt")))
                .toList();

        int total = userFiles.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<Map<String, Object>> content = fromIndex < total
                ? userFiles.subList(fromIndex, toIndex).stream()
                        .map(f -> {
                            Map<String, Object> item = new HashMap<>(f);
                            item.remove("content");
                            return item;
                        })
                        .toList()
                : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("size", size);
        result.put("number", page - 1);
        result.put("first", page == 1);
        result.put("last", toIndex >= total);
        result.put("empty", content.isEmpty());

        return result;
    }

    /**
     * 删除文件
     */
    public Map<String, Object> deleteFile(String fileId) {
        Map<String, Object> removed = FILE_STORE.remove(fileId);
        if (removed == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}