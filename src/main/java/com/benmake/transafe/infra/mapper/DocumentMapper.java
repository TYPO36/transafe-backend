package com.benmake.transafe.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.document.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 文档 Mapper
 *
 * @author JTP
 * @date 2026-04-01
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    /**
     * 根据fileId查询文档
     *
     * @param fileId 文件唯一标识
     * @return 文档实体
     */
    @Select("SELECT * FROM document WHERE file_id = #{fileId}")
    Optional<DocumentEntity> findByFileId(@Param("fileId") String fileId);

    /**
     * 根据fileId查询文档（合并后直接查询document表）
     *
     * @param fileId 文件唯一标识
     * @return 文档视图对象
     */
    @Select("SELECT id, file_id, user_id, parent_id, root_id, " +
            "file_name, file_size, file_type, storage_path, status, " +
            "parse_status, parse_error_code, parse_error_message, password_provided, " +
            "is_attachment, priority, retry_count, " +
            "created_at, updated_at " +
            "FROM document WHERE file_id = #{fileId}")
    Optional<DocumentVO> findByFileIdWithFile(@Param("fileId") String fileId);

    /**
     * 根据父ID查询子文档列表
     *
     * @param parentId 父文档fileId
     * @return 子文档列表
     */
    @Select("SELECT * FROM document WHERE parent_id = #{parentId}")
    List<DocumentEntity> findByParentId(@Param("parentId") String parentId);

    /**
     * 根据根ID查询所有关联文档
     *
     * @param rootId 根文档fileId
     * @return 关联文档列表
     */
    @Select("SELECT * FROM document WHERE root_id = #{rootId}")
    List<DocumentEntity> findByRootId(@Param("rootId") String rootId);

    /**
     * 根据解析状态查询文档列表
     *
     * @param parseStatus 解析状态
     * @return 文档列表
     */
    @Select("SELECT * FROM document WHERE parse_status = #{parseStatus}")
    List<DocumentEntity> findByParseStatus(@Param("parseStatus") String parseStatus);

    /**
     * 判断文档是否存在
     *
     * @param fileId 文件唯一标识
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM document WHERE file_id = #{fileId}")
    boolean existsByFileId(@Param("fileId") String fileId);

    /**
     * 统计根文档下各状态的文档数量
     *
     * @param rootId 根文档fileId
     * @param parseStatus 解析状态
     * @return 文档数量
     */
    @Select("SELECT COUNT(*) FROM document WHERE root_id = #{rootId} AND parse_status = #{parseStatus}")
    long countByRootIdAndParseStatus(@Param("rootId") String rootId, @Param("parseStatus") String parseStatus);

    /**
     * 根据document主键ID查询文档
     *
     * @param documentId document主键ID
     * @return 文档实体
     */
    @Select("SELECT * FROM document WHERE id = #{documentId}")
    Optional<DocumentEntity> findByDocumentId(@Param("documentId") Long documentId);
}
