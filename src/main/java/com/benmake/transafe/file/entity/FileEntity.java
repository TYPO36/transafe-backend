package com.benmake.transafe.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 文件实体
 *
 * @author JTP
 * @date 2026-04-01
 */
@Data
@TableName("file")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识
     */
    @TableField("file_id")
    private String fileId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型/扩展名
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件存储路径（相对路径）
     */
    @TableField("storage_path")
    private String storagePath;

    /**
     * 文件状态：UPLOADED-已上传，DELETED-已删除
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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