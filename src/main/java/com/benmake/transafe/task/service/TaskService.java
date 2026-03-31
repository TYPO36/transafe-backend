package com.benmake.transafe.task.service;

import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.file.service.FileProxyService;
import com.benmake.transafe.quota.service.QuotaService;
import com.benmake.transafe.task.dto.TaskCreateRequest;
import com.benmake.transafe.task.dto.TaskResponse;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 任务服务
 *
 * @author TYPO
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskProducer taskProducer;
    private final QuotaService quotaService;
    private final FileProxyService fileProxyService;

    /**
     * 创建解析任务
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, Long userId) {
        // 获取文件信息
        Map<String, Object> fileInfo = fileProxyService.getFileInfo(request.getFileId());

        // 创建任务记录
        TaskEntity task = new TaskEntity();
        task.setTaskId(taskProducer.generateTaskId());
        task.setUserId(userId);
        task.setFileId(request.getFileId());
        task.setFileName((String) fileInfo.get("fileName"));
        task.setFileType((String) fileInfo.get("fileType"));
        task.setStatus("PENDING");

        task = taskRepository.save(task);

        // 发送任务消息
        taskProducer.sendParseTask(task);

        log.info("创建任务成功: taskId={}, userId={}", task.getTaskId(), userId);

        return toResponse(task);
    }

    /**
     * 获取任务详情
     */
    public TaskResponse getTask(String taskId, Long userId) {
        TaskEntity task = taskRepository.findByTaskId(taskId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        return toResponse(task);
    }

    /**
     * 获取任务列表
     */
    public Page<TaskResponse> listTasks(Long userId, int page, int size, String status) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<TaskEntity> tasks;
        if (status != null && !status.isEmpty()) {
            tasks = taskRepository.findByUserIdAndStatus(userId, status, pageRequest);
        } else {
            tasks = taskRepository.findByUserId(userId, pageRequest);
        }

        return tasks.map(this::toResponse);
    }

    private TaskResponse toResponse(TaskEntity task) {
        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .fileId(task.getFileId())
                .fileName(task.getFileName())
                .fileType(task.getFileType())
                .status(task.getStatus())
                .charCount(task.getCharCount())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                .build();
    }
}