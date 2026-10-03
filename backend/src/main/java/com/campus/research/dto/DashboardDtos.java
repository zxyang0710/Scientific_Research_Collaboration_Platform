package com.campus.research.dto;

import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardView(
            long projectCount,
            long activeProjectCount,
            long taskCount,
            long completedTaskCount,
            long overdueTaskCount,
            long paperCount,
            long unreadNotificationCount,
            List<ProjectDtos.ProjectView> recentProjects,
            List<TaskDtos.TaskView> myTasks,
            List<ActivityItem> activities,
            List<ChartItem> taskDistribution,
            List<ChartItem> projectProgress) {
    }

    public record ActivityItem(
            String type,
            String title,
            String description,
            String actorName,
            Long projectId,
            LocalDate date) {
    }

    public record ChartItem(String name, long value) {
    }
}
