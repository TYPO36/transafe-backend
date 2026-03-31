package com.benmake.transafe.document.entity;

import com.benmake.transafe.document.common.constant.ParseStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Entity
@Table(name = "document", indexes = {
    @Index(name = "idx_file_id", columnList = "file_id", unique = true),
    @Index(name = "idx_root_id", columnList = "root_id"),
    @Index(name = "idx_parent_id", columnList = "parent_id"),
    @Index(name = "idx_parse_status", columnList = "parse_status"),
    @Index(name = "idx_priority", columnList = "priority"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件唯一标识(UUID)
     */
    @Column(name = "file_id", unique = true, nullable = false, length = 64)
    private String fileId;

    /**
     * 父文档file_id，顶层为null
     */
    @Column(name = "parent_id", length = 64)
    private String parentId;

    /**
     * 根文档file_id，所有关联文档指向顶层
     */
    @Column(name = "root_id", length = 64)
    private String rootId;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    /**
     * 文件大小(字节)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 文件存储路径
     */
    @Column(name = "file_storage_path", nullable = false, length = 1024)
    private String fileStoragePath;

    /**
     * 文件类型: pdf,doc,docx,ppt,pptx,xls,xlsx,txt,eml
     */
    @Column(name = "file_type", nullable = false, length = 32)
    private String fileType;

    /**
     * 状态: pending/parsing/parsed/failed
     */
    @Column(name = "parse_status", length = 32)
    private String parseStatus = ParseStatus.PENDING;

    /**
     * 错误码: 0成功, 3001密码保护, 3002不支持格式, 3003文件损坏, 3004解析超时
     */
    @Column(name = "parse_error_code")
    private Integer parseErrorCode = 0;

    /**
     * 错误信息
     */
    @Column(name = "parse_error_message", length = 1024)
    private String parseErrorMessage;

    /**
     * 用户提供的密码(仅密码保护文件)
     */
    @Column(name = "password_provided", length = 256)
    private String passwordProvided;

    /**
     * 是否为附件
     */
    @Column(name = "is_attachment")
    private Boolean isAttachment = false;

    /**
     * 优先级: 0普通, 1优先
     */
    @Column(name = "priority")
    private Integer priority = 0;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
