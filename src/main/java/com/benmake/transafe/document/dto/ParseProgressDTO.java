package com.benmake.transafe.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析进度DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseProgressDTO {

    /**
     * 根文档ID
     */
    private String rootId;

    /**
     * 总文件数（包括 ZIP 包内的子文件）
     */
    private Long total;

    /**
     * 待处理数量
     */
    private Long pending;

    /**
     * 解析中数量
     */
    private Long parsing;

    /**
     * 解析成功数量
     */
    private Long success;

    /**
     * 解析失败数量
     */
    private Long failed;

    /**
     * 完成百分比 (0-100)
     */
    private Integer progress;

    /**
     * 是否全部完成
     */
    private Boolean completed;
}
