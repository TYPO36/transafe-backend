package com.benmake.transafe.task.repository;

import com.benmake.transafe.task.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 任务 Repository
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findByTaskId(String taskId);

    Page<TaskEntity> findByUserId(Long userId, Pageable pageable);

    Page<TaskEntity> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
}