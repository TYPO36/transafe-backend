package com.benmake.transafe.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {

    /**
     * 总命中数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 搜索结果项列表
     */
    @Builder.Default
    private List<SearchItem> items = new ArrayList<>();

    /**
     * 搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {

        /**
         * 文件唯一标识
         */
        private String fileId;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件类型
         */
        private String fileType;

        /**
         * 高亮内容
         */
        private String highlight;

        /**
         * 相关性得分
         */
        private Float score;

        /**
         * 是否为附件
         */
        private Boolean isAttachment;

        /**
         * 子文档（附件）列表
         */
        @Builder.Default
        private List<SearchItem> children = new ArrayList<>();
    }
}
