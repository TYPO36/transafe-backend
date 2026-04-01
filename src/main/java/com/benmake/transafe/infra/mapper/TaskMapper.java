package com.benmake.transafe.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.task.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 任务 Mapper
 *
 * @author JTP
 * @date 2026-04-01
 */
@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {

    /**
     * 根据任务ID查询任务
     *
     * @param taskId 任务ID
     * @return 任务实体
     */
    @Select("SELECT * FROM task WHERE task_id = #{taskId}")
    Optional<TaskEntity> findByTaskId(@Param("taskId") String taskId);

    /**
     * 根据用户ID分页查询任务
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @return 任务分页列表
     */
    @Select("SELECT * FROM task WHERE user_id = #{userId} ORDER BY created_at DESC")
    IPage<TaskEntity> findByUserId(Page<TaskEntity> page, @Param("userId") Long userId);

    /**
     * 根据用户ID和状态分页查询任务
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param status 状态
     * @return 任务分页列表
     */
    @Select("SELECT * FROM task WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    IPage<TaskEntity> findByUserIdAndStatus(Page<TaskEntity> page, @Param("userId") Long userId, @Param("status") String status);
}