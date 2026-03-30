package com.benmake.transafe.task.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Entity
@Table(name = "task", indexes = {
        @Index(name = "idx_task_id", columnList = "task_id"),
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status")
})
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, nullable = false, length = 50)
    private String taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_id", nullable = false, length = 50)
    private String fileId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "char_count")
    private Integer charCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}