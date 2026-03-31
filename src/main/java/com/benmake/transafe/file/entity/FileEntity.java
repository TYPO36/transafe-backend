package com.benmake.transafe.file.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 文件实体
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Entity
@Table(name = "file", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_file_id", columnList = "file_id", unique = true),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件唯一标识
     */
    @Column(name = "file_id", unique = true, nullable = false, length = 64)
    private String fileId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件类型/扩展名
     */
    @Column(name = "file_type", length = 32)
    private String fileType;

    /**
     * 文件存储路径（相对路径）
     */
    @Column(name = "storage_path", length = 500)
    private String storagePath;

    /**
     * 文件状态：UPLOADED-已上传，DELETED-已删除
     */
    @Column(name = "status", length = 32)
    private String status;

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
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "UPLOADED";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
