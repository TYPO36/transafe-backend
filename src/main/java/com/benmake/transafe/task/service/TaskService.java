package com.benmake.transafe.task.service;

import com.benmake.transafe.task.dto.TaskCreateRequest;
import com.benmake.transafe.task.dto.TaskResponse;

import java.util.Map;

/**
 * 任务服务接口
 *
 * @author JTP
 * @date 2026-04-01
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
     * @param pageNum 页码
     * @param size 每页数量
     * @param status 状态筛选
     * @return 分页任务列表
     */
    Map<String, Object> listTasks(Long userId, int pageNum, int size, String status);
}