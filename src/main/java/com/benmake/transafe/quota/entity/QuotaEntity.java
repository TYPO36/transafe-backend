package com.benmake.transafe.quota.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted = 0;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version = 0;
}