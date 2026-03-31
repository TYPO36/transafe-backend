package com.benmake.transafe.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传请求DTO
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    /**
     * 是否为附件（可选，默认false）
     */
    private Boolean isAttachment;

    /**
     * 父文档ID（可选，用于附件）
     */
    @Size(max = 64, message = "父文档ID长度不能超过64")
    private String parentId;
}
