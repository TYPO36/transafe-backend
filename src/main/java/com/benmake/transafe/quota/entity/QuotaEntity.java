package com.benmake.transafe.quota.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配额实体
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@TableName("quota")
public class QuotaEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("daily_translation_total")
    private Integer dailyTranslationTotal = 5000;

    @TableField("daily_translation_used")
    private Integer dailyTranslationUsed = 0;

    @TableField("storage_total")
    private Long storageTotal = 5368709120L;

    @TableField("storage_used")
    private Long storageUsed = 0L;

    @TableField("last_reset_date")
    private LocalDate lastResetDate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}