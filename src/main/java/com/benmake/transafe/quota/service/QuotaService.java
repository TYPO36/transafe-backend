package com.benmake.transafe.quota.service;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.quota.dto.QuotaStatusResponse;
import com.benmake.transafe.quota.entity.QuotaEntity;
import com.benmake.transafe.quota.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 配额服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaRepository quotaRepository;

    /**
     * 获取用户配额状态
     */
    public QuotaStatusResponse getQuotaStatus(Long userId) {
        QuotaEntity quota = quotaRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultQuota(userId));

        // 检查是否需要重置每日配额
        if (!LocalDate.now().equals(quota.getLastResetDate())) {
            quota.setDailyTranslationUsed(0);
            quota.setLastResetDate(LocalDate.now());
            quotaRepository.save(quota);
        }

        return QuotaStatusResponse.builder()
                .dailyQuota(QuotaStatusResponse.DailyQuota.builder()
                        .used(quota.getDailyTranslationUsed())
                        .total(quota.getDailyTranslationTotal())
                        .remaining(quota.getDailyTranslationTotal() - quota.getDailyTranslationUsed())
                        .build())
                .storageQuota(QuotaStatusResponse.StorageQuota.builder()
                        .used(quota.getStorageUsed())
                        .total(quota.getStorageTotal())
                        .remaining(quota.getStorageTotal() - quota.getStorageUsed())
                        .build())
                .build();
    }

    /**
     * 消耗翻译配额
     */
    @Transactional
    public void consumeTranslationQuota(Long userId, int charCount) {
        QuotaEntity quota = quotaRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultQuota(userId));

        // 检查是否需要重置每日配额
        if (!LocalDate.now().equals(quota.getLastResetDate())) {
            quota.setDailyTranslationUsed(0);
            quota.setLastResetDate(LocalDate.now());
        }

        if (quota.getDailyTranslationUsed() + charCount > quota.getDailyTranslationTotal()) {
            throw new BusinessException("QUOTA_EXCEEDED", "翻译配额不足");
        }

        quota.setDailyTranslationUsed(quota.getDailyTranslationUsed() + charCount);
        quotaRepository.save(quota);
        log.info("用户 {} 消耗翻译配额: {} 字符", userId, charCount);
    }

    /**
     * 检查存储空间
     */
    public boolean checkStorageSpace(Long userId, long fileSize) {
        QuotaEntity quota = quotaRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultQuota(userId));

        return quota.getStorageUsed() + fileSize <= quota.getStorageTotal();
    }

    /**
     * 更新存储使用量
     */
    @Transactional
    public void updateStorageUsed(Long userId, long delta) {
        QuotaEntity quota = quotaRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultQuota(userId));

        quota.setStorageUsed(Math.max(0, quota.getStorageUsed() + delta));
        quotaRepository.save(quota);
        log.info("用户 {} 存储使用量更新: {}, 当前: {}", userId, delta, quota.getStorageUsed());
    }

    /**
     * 创建默认配额
     */
    @Transactional
    public QuotaEntity createDefaultQuota(Long userId) {
        QuotaEntity quota = new QuotaEntity();
        quota.setUserId(userId);
        quota.setDailyTranslationTotal(5000);
        quota.setDailyTranslationUsed(0);
        quota.setStorageTotal(5368709120L);
        quota.setStorageUsed(0L);
        quota.setLastResetDate(LocalDate.now());
        return quotaRepository.save(quota);
    }
}