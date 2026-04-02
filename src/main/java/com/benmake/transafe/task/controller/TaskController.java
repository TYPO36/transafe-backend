package com.benmake.transafe.task.controller;

import com.benmake.transafe.common.response.ApiResponse;
import com.benmake.transafe.task.dto.TaskCreateRequest;
import com.benmake.transafe.task.dto.TaskResponse;
import com.benmake.transafe.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 任务控制器
 *
 * @author JTP
 * @date 2026-04-01
 */
@Tag(name = "任务", description = "翻译任务的创建、查询、列表等接口")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "创建任务", description = "创建一个新的翻译任务")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestBody TaskCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        TaskResponse response = taskService.createTask(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "获取任务详情", description = "根据任务ID获取任务详情")
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        TaskResponse response = taskService.getTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "获取任务列表", description = "获取当前用户的任务列表，支持分页和状态筛选")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTasks(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "任务状态筛选（可选）") @RequestParam(required = false) String status) {
        Map<String, Object> tasks = taskService.listTasks(userId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}