package com.campus.research.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotNull(message = "请选择所属项目") Long projectId,
            @NotBlank(message = "任务标题不能为空")
            @Size(max = 150, message = "任务标题不能超过 150 个字符") String title,
            String description,
            Long assigneeId,
            String priority,
            String status,
            @Min(value = 0, message = "进度不能小于 0")
            @Max(value = 100, message = "进度不能大于 100") Integer progress,
            LocalDate deadline) {
    }

    public record TaskView(
            Long id,
            Long projectId,
            String projectName,
            String title,
            String description,
            Long assigneeId,
            String assigneeName,
            String assigneeAvatar,
            Long creatorId,
            String creatorName,
            String priority,
            String status,
            Integer progress,
            LocalDate deadline,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
