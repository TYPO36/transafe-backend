package com.benmake.transafe.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ消息DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 父文档file_id
     */
    private String parentId;

    /**
     * 根文档file_id
     */
    private String rootId;

    /**
     * 文件存储路径
     */
    private String fileStoragePath;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 是否为附件
     */
    private Boolean isAttachment;

    /**
     * 密码（可选）
     */
    private String password;

    /**
     * 优先级: 0普通, 1优先
     */
    private Integer priority;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}
