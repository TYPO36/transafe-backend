package com.benmake.transafe.task.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.file.dto.FileInfoResponse;
import com.benmake.transafe.file.service.FileProxyService;
import com.benmake.transafe.infra.mapper.TaskMapper;
import com.benmake.transafe.quota.service.QuotaService;
import com.benmake.transafe.task.dto.TaskCreateRequest;
import com.benmake.transafe.task.dto.TaskResponse;
import com.benmake.transafe.task.entity.TaskEntity;
import com.benmake.transafe.task.service.TaskProducer;
import com.benmake.transafe.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务服务实现
 *
 * @author JTP
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskProducer taskProducer;
    private final QuotaService quotaService;
    private final FileProxyService fileProxyService;

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, Long userId) {
        // 获取文件信息
        FileInfoResponse fileInfo = fileProxyService.getFileInfo(request.getFileId(), userId);

        // 创建任务记录
        TaskEntity task = new TaskEntity();
        task.setTaskId(taskProducer.generateTaskId());
        task.setUserId(userId);
        task.setFileId(request.getFileId());
        task.setFileName(fileInfo.getFileName());
        task.setFileType(fileInfo.getFileType());
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());

        taskMapper.insert(task);

        // 发送任务消息
        taskProducer.sendParseTask(task);

        log.info("创建任务成功: taskId={}, userId={}", task.getTaskId(), userId);

        return toResponse(task);
    }

    @Override
    public TaskResponse getTask(String taskId, Long userId) {
        TaskEntity task = taskMapper.findByTaskId(taskId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        return toResponse(task);
    }

    @Override
    public Map<String, Object> listTasks(Long userId, int pageNum, int size, String status) {
        Page<TaskEntity> page = new Page<>(pageNum, size);
        IPage<TaskEntity> tasks;

        if (status != null && !status.isEmpty()) {
            tasks = taskMapper.findByUserIdAndStatus(page, userId, status);
        } else {
            tasks = taskMapper.findByUserId(page, userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", tasks.getRecords().stream().map(this::toResponse).toList());
        result.put("totalElements", tasks.getTotal());
        result.put("totalPages", tasks.getPages());
        result.put("size", tasks.getSize());
        result.put("number", tasks.getCurrent() - 1);
        result.put("first", tasks.getCurrent() == 1);
        result.put("last", tasks.getCurrent() == tasks.getPages());
        result.put("empty", tasks.getRecords().isEmpty());

        return result;
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