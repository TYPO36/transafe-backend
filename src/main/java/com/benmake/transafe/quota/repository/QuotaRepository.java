package com.benmake.transafe.quota.repository;

import com.benmake.transafe.quota.entity.QuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 配额 Repository
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Repository
public interface QuotaRepository extends JpaRepository<QuotaEntity, Long> {

    Optional<QuotaEntity> findByUserId(Long userId);
}