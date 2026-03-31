package com.benmake.transafe.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档树DTO（支持递归children）
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTreeDTO {

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 父文档file_id，顶层为null
     */
    private String parentId;

    /**
     * 根文档file_id
     */
    private String rootId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 解析状态: pending/parsing/parsed/failed
     */
    private String parseStatus;

    /**
     * 错误码
     */
    private Integer parseErrorCode;

    /**
     * 错误信息
     */
    private String parseErrorMessage;

    /**
     * 是否有密码保护
     */
    private Boolean hasPassword;

    /**
     * 是否为附件
     */
    private Boolean isAttachment;

    /**
     * 优先级: 0普通, 1优先
     */
    private Integer priority;

    /**
     * 子文档列表（递归）
     */
    @Builder.Default
    private List<DocumentTreeDTO> children = new ArrayList<>();
}
