package com.campus.research.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank(message = "项目名称不能为空")
            @Size(max = 120, message = "项目名称不能超过 120 个字符") String name,
            @NotBlank(message = "项目编号不能为空") String code,
            String description,
            String category,
            String source,
            Long leaderId,
            String status,
            @Min(value = 0, message = "进度不能小于 0")
            @Max(value = 100, message = "进度不能大于 100") Integer progress,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget) {
    }

    public record ProjectView(
            Long id,
            String name,
            String code,
            String description,
            String category,
            String source,
            Long leaderId,
            String leaderName,
            String status,
            Integer progress,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            Long memberCount,
            Long taskCount,
            Long completedTaskCount,
            String projectRole,
            LocalDateTime updatedAt) {
    }

    public record MemberRequest(
            @NotNull(message = "请选择用户") Long userId,
            @NotBlank(message = "请选择项目角色") String projectRole) {
    }

    public record MemberView(
            Long id,
            Long userId,
            String username,
            String realName,
            String department,
            String identity,
            String avatar,
            String projectRole,
            LocalDateTime joinedAt) {
    }

    public record ProjectDetail(
            ProjectView project,
            List<MemberView> members,
            List<TaskDtos.TaskView> tasks,
            List<PaperDtos.PaperView> papers,
            long fileCount,
            long discussionCount) {
    }
}
