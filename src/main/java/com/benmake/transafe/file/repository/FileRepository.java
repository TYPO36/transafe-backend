package com.benmake.transafe.file.repository;

import com.benmake.transafe.file.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 文件仓库
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    /**
     * 根据文件ID查找
     */
    Optional<FileEntity> findByFileId(String fileId);

    /**
     * 根据文件ID和用户ID查找
     */
    Optional<FileEntity> findByFileIdAndUserId(String fileId, Long userId);

    /**
     * 根据用户ID分页查询文件列表
     */
    Page<FileEntity> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    /**
     * 根据用户ID查询文件列表
     */
    Page<FileEntity> findByUserId(Long userId, Pageable pageable);

    /**
     * 统计用户的文件数量
     */
    long countByUserIdAndStatus(Long userId, String status);

    /**
     * 批量删除用户文件（软删除）
     */
    @Modifying
    @Query("UPDATE FileEntity f SET f.status = 'DELETED' WHERE f.userId = :userId AND f.fileId IN :fileIds")
    int batchDeleteByUserId(@Param("userId") Long userId, @Param("fileIds") java.util.List<String> fileIds);
}
