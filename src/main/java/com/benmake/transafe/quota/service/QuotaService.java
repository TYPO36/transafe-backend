package com.benmake.transafe.quota.service;

import com.benmake.transafe.quota.dto.QuotaStatusResponse;
import com.benmake.transafe.quota.entity.QuotaEntity;

/**
 * 配额服务接口
 *
 * @author TYPO
 * @since 2026-03-30
 */
public interface QuotaService {

    /**
     * 获取用户配额状态
     *
     * @param userId 用户ID
     * @return 配额状态响应
     */
    QuotaStatusResponse getQuotaStatus(Long userId);

    /**
     * 消耗翻译配额
     *
     * @param userId 用户ID
     * @param charCount 消耗的字符数
     */
    void consumeTranslationQuota(Long userId, int charCount);

    /**
     * 检查存储空间
     *
     * @param userId 用户ID
     * @param fileSize 文件大小
     * @return 是否充足
     */
    boolean checkStorageSpace(Long userId, long fileSize);

    /**
     * 更新存储使用量
     *
     * @param userId 用户ID
     * @param delta 变化量（正数增加，负数减少）
     */
    void updateStorageUsed(Long userId, long delta);

    /**
     * 创建默认配额
     *
     * @param userId 用户ID
     * @return 创建的配额实体
     */
    QuotaEntity createDefaultQuota(Long userId);
}
