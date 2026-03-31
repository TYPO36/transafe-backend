package com.benmake.transafe.task.service;

import com.benmake.transafe.task.dto.TaskCreateRequest;
import com.benmake.transafe.task.dto.TaskResponse;
import org.springframework.data.domain.Page;

/**
 * 任务服务接口
 *
 * @author TYPO
 * @since 2026-03-30
 */
public interface TaskService {

    /**
     * 创建解析任务
     *
     * @param request 任务创建请求
     * @param userId 用户ID
     * @return 任务响应
     */
    TaskResponse createTask(TaskCreateRequest request, Long userId);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务响应
     */
    TaskResponse getTask(String taskId, Long userId);

    /**
     * 获取任务列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @param status 状态筛选
     * @return 分页任务列表
     */
    Page<TaskResponse> listTasks(Long userId, int page, int size, String status);
}
