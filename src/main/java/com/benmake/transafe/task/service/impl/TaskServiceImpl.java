package com.benmake.transafe.task.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benmake.transafe.common.exception.BusinessException;
import com.benmake.transafe.common.exception.ErrorCode;
import com.benmake.transafe.document.entity.DocumentEntity;
import com.benmake.transafe.infra.mapper.DocumentMapper;
import com.benmake.transafe.infra.mapper.TaskMapper;
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
    private final DocumentMapper documentMapper;
    private final TaskProducer taskProducer;

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, Long userId) {
        // 根据 fileId 查询 document 获取 documentId
        DocumentEntity doc = documentMapper.findByFileId(request.getFileId())
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // 创建任务记录
        TaskEntity task = new TaskEntity();
        task.setTaskId(taskProducer.generateTaskId());
        task.setUserId(userId);
        task.setDocumentId(doc.getId());
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());

        taskMapper.insert(task);

        // 发送任务消息
        taskProducer.sendParseTask(task);

        log.info("创建任务成功: taskId={}, userId={}, documentId={}", task.getTaskId(), userId, doc.getId());

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
        // 通过 documentId 查询 document 获取文件信息
        String fileName = null;
        String fileType = null;
        Integer charCount = null;

        if (task.getDocumentId() != null) {
            DocumentEntity doc = documentMapper.findByDocumentId(task.getDocumentId()).orElse(null);
            if (doc != null) {
                fileName = doc.getFileName();
                fileType = doc.getFileType();
            }
        }

        // 从 result JSON 解析 charCount
        if (task.getResult() != null && task.getResult().contains("charCount")) {
            try {
                String resultJson = task.getResult();
                int start = resultJson.indexOf("\"charCount\":") + 12;
                int end = resultJson.indexOf("}", start);
                charCount = Integer.parseInt(resultJson.substring(start, end).trim());
            } catch (Exception e) {
                log.warn("解析task result失败: taskId={}, result={}", task.getTaskId(), task.getResult());
            }
        }

        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .documentId(task.getDocumentId())
                .fileName(fileName)
                .fileType(fileType)
                .status(task.getStatus())
                .charCount(charCount)
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                .build();
    }
}
