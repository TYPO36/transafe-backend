package com.benmake.transafe.document.repository;

import com.benmake.transafe.document.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档Repository
 *
 * @author TYPO
 * @date 2026-03-31
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    /**
     * 根据fileId查询文档
     *
     * @param fileId 文件唯一标识
     * @return 文档实体
     */
    Optional<DocumentEntity> findByFileId(String fileId);

    /**
     * 根据父ID查询子文档列表
     *
     * @param parentId 父文档fileId
     * @return 子文档列表
     */
    List<DocumentEntity> findByParentId(String parentId);

    /**
     * 根据根ID查询所有关联文档
     *
     * @param rootId 根文档fileId
     * @return 关联文档列表
     */
    List<DocumentEntity> findByRootId(String rootId);

    /**
     * 根据解析状态查询文档列表
     *
     * @param parseStatus 解析状态
     * @return 文档列表
     */
    List<DocumentEntity> findByParseStatus(String parseStatus);

    /**
     * 判断文档是否存在
     *
     * @param fileId 文件唯一标识
     * @return 是否存在
     */
    boolean existsByFileId(String fileId);

    /**
     * 统计根文档下各状态的文档数量
     *
     * @param rootId 根文档fileId
     * @param parseStatus 解析状态
     * @return 文档数量
     */
    long countByRootIdAndParseStatus(String rootId, String parseStatus);
}
