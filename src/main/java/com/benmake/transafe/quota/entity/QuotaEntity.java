package com.benmake.transafe.quota.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配额实体
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Entity
@Table(name = "quota")
public class QuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "daily_translation_total")
    private Integer dailyTranslationTotal = 5000;

    @Column(name = "daily_translation_used")
    private Integer dailyTranslationUsed = 0;

    @Column(name = "storage_total")
    private Long storageTotal = 5368709120L; // 5GB

    @Column(name = "storage_used")
    private Long storageUsed = 0L;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.lastResetDate == null) {
            this.lastResetDate = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}