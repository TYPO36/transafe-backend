package com.benmake.transafe.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量上传响应DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    /**
     * 根文档ID（用于查询解析进度）
     */
    private String rootId;

    /**
     * 总文件数
     */
    private Integer total;

    /**
     * 成功数量
     */
    private Integer success;

    /**
     * 失败数量
     */
    private Integer failed;

    /**
     * 上传结果项列表
     */
    @Builder.Default
    private List<UploadItem> items = new ArrayList<>();

    /**
     * 上传结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadItem {

        /**
         * 文件唯一标识
         */
        private String fileId;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 解析状态
         */
        private String status;
    }
}
