package com.benmake.transafe.quota.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配额状态响应
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaStatusResponse {

    private DailyQuota dailyQuota;
    private StorageQuota storageQuota;
    private Integer membershipLevel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyQuota {
        private Integer used;
        private Integer total;
        private Integer remaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageQuota {
        private Long used;
        private Long total;
        private Long remaining;
    }
}