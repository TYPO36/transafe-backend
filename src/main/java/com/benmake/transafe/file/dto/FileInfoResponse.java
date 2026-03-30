package com.benmake.transafe.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件信息响应
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String status;
    private String createdAt;
}