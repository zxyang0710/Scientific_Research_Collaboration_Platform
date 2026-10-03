package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.common.PageResult;
import com.campus.research.dto.TaskDtos.TaskRequest;
import com.campus.research.dto.TaskDtos.TaskView;
import com.campus.research.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ApiResponse<PageResult<TaskView>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(taskService.list(projectId, status, priority, assigneeId, mine, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskView> get(@PathVariable Long id) {
        return ApiResponse.success(taskService.get(id));
    }

    @PostMapping
    public ApiResponse<TaskView> create(@Valid @RequestBody TaskRequest request) {
        return ApiResponse.success("任务创建成功", taskService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskView> update(@PathVariable Long id,
                                        @Valid @RequestBody TaskRequest request) {
        return ApiResponse.success("任务更新成功", taskService.update(id, request));
    }

    @PutMapping("/{id}/progress")
    public ApiResponse<TaskView> updateProgress(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        Integer progress = body.get("progress") == null
                ? null : Integer.valueOf(String.valueOf(body.get("progress")));
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        return ApiResponse.success("任务进度更新成功", taskService.updateProgress(id, progress, status));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.success();
    }
}
