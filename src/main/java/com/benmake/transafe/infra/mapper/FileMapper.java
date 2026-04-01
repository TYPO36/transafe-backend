package com.benmake.transafe.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.file.entity.FileEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * 文件 Mapper
 *
 * @author JTP
 * @date 2026-04-01
 */
@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {

    /**
     * 根据文件ID查询文件
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    @Select("SELECT * FROM file WHERE file_id = #{fileId}")
    Optional<FileEntity> findByFileId(@Param("fileId") String fileId);

    /**
     * 根据文件ID和用户ID查询文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件实体
     */
    @Select("SELECT * FROM file WHERE file_id = #{fileId} AND user_id = #{userId}")
    Optional<FileEntity> findByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") Long userId);

    /**
     * 根据用户ID和状态分页查询文件
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 状态
     * @return 文件分页列表
     */
    @Select("SELECT * FROM file WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    IPage<FileEntity> findByUserIdAndStatus(Page<FileEntity> page, @Param("userId") Long userId, @Param("status") String status);

    /**
     * 根据用户ID分页查询文件
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @return 文件分页列表
     */
    @Select("SELECT * FROM file WHERE user_id = #{userId} ORDER BY created_at DESC")
    IPage<FileEntity> findByUserId(Page<FileEntity> page, @Param("userId") Long userId);

    /**
     * 统计用户的文件数量
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM file WHERE user_id = #{userId} AND status = #{status}")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 批量软删除用户文件
     *
     * @param userId 用户ID
     * @param fileIds 文件ID列表
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE file SET status = 'DELETED', updated_at = NOW() " +
            "WHERE user_id = #{userId} AND file_id IN " +
            "<foreach collection='fileIds' item='fileId' open='(' separator=',' close=')'>" +
            "#{fileId}" +
            "</foreach>" +
            "</script>")
    int batchDeleteByUserId(@Param("userId") Long userId, @Param("fileIds") List<String> fileIds);

    /**
     * 插入文件实体（供解析服务创建解压文件、附件等场景使用）
     *
     * @param fileEntity 文件实体
     * @return 插入行数
     */
    @Insert("INSERT INTO file (file_id, user_id, file_name, file_size, file_type, storage_path, status, created_at, updated_at) " +
            "VALUES (#{fileId}, #{userId}, #{fileName}, #{fileSize}, #{fileType}, #{storagePath}, #{status}, #{createdAt}, #{updatedAt})")
    int insertFileEntity(FileEntity fileEntity);
}