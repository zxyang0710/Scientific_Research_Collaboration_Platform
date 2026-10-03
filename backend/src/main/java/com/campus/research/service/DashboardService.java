package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.research.dto.DashboardDtos.ActivityItem;
import com.campus.research.dto.DashboardDtos.ChartItem;
import com.campus.research.dto.DashboardDtos.DashboardView;
import com.campus.research.dto.ProjectDtos.ProjectView;
import com.campus.research.dto.TaskDtos.TaskView;
import com.campus.research.entity.ProjectDiscussion;
import com.campus.research.entity.ProjectFile;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchPaper;
import com.campus.research.entity.ResearchProject;
import com.campus.research.entity.ResearchTask;
import com.campus.research.entity.User;
import com.campus.research.mapper.ProjectDiscussionMapper;
import com.campus.research.mapper.ProjectFileMapper;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchPaperMapper;
import com.campus.research.mapper.ResearchProjectMapper;
import com.campus.research.mapper.ResearchTaskMapper;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchTaskMapper taskMapper;
    private final ResearchPaperMapper paperMapper;
    private final ProjectDiscussionMapper discussionMapper;
    private final ProjectFileMapper fileMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final MappingService mappingService;

    public DashboardView dashboard() {
        Long currentUserId = SecurityUtils.currentUserId();
        Set<Long> projectIds = SecurityUtils.isAdmin()
                ? projectMapper.selectList(null).stream().map(ResearchProject::getId).collect(Collectors.toSet())
                : memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getUserId, currentUserId)).stream()
                .map(ProjectMember::getProjectId).collect(Collectors.toSet());

        if (projectIds.isEmpty()) {
            return new DashboardView(0, 0, 0, 0, 0, 0,
                    notificationService.unreadCount(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<ResearchProject> projects = projectMapper.selectList(Wrappers.<ResearchProject>lambdaQuery()
                .in(ResearchProject::getId, projectIds)
                .orderByDesc(ResearchProject::getUpdatedAt));
        List<ResearchTask> tasks = taskMapper.selectList(Wrappers.<ResearchTask>lambdaQuery()
                .in(ResearchTask::getProjectId, projectIds));
        List<ResearchTask> myTasks = taskMapper.selectList(Wrappers.<ResearchTask>lambdaQuery()
                .in(ResearchTask::getProjectId, projectIds)
                .eq(ResearchTask::getAssigneeId, currentUserId)
                .ne(ResearchTask::getStatus, "DONE")
                .orderByAsc(ResearchTask::getDeadline)
                .last("LIMIT 8"));
        long paperCount = paperMapper.selectCount(Wrappers.<ResearchPaper>lambdaQuery()
                .in(ResearchPaper::getProjectId, projectIds));
        long overdue = tasks.stream().filter(task -> task.getDeadline() != null
                && task.getDeadline().isBefore(LocalDate.now())
                && !"DONE".equals(task.getStatus())).count();
        List<ProjectView> recentProjects = mappingService.toProjectViews(projects.stream().limit(5).toList());
        List<TaskView> myTaskViews = mappingService.toTaskViews(myTasks);

        return new DashboardView(projects.size(),
                projects.stream().filter(project -> "IN_PROGRESS".equals(project.getStatus())).count(),
                tasks.size(), tasks.stream().filter(task -> "DONE".equals(task.getStatus())).count(),
                overdue, paperCount, notificationService.unreadCount(), recentProjects, myTaskViews,
                activities(projectIds), taskDistribution(tasks), projectProgress(projects));
    }

    private List<ActivityItem> activities(Set<Long> projectIds) {
        List<ActivityItem> activities = new ArrayList<>();
        Map<Long, ResearchProject> projects = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(ResearchProject::getId, project -> project));

        List<ProjectDiscussion> discussions = discussionMapper.selectList(Wrappers.<ProjectDiscussion>lambdaQuery()
                .in(ProjectDiscussion::getProjectId, projectIds)
                .orderByDesc(ProjectDiscussion::getCreatedAt)
                .last("LIMIT 8"));
        Map<Long, User> users = loadUsers(discussions.stream().map(ProjectDiscussion::getAuthorId)
                .collect(Collectors.toSet()));
        discussions.forEach(item -> activities.add(new ActivityItem("DISCUSSION",
                "发布项目讨论", shorten(item.getContent()),
                user(users, item.getAuthorId()), item.getProjectId(),
                item.getCreatedAt().toLocalDate())));

        List<ResearchPaper> papers = paperMapper.selectList(Wrappers.<ResearchPaper>lambdaQuery()
                .in(ResearchPaper::getProjectId, projectIds)
                .orderByDesc(ResearchPaper::getUpdatedAt)
                .last("LIMIT 5"));
        Map<Long, User> paperUsers = loadUsers(papers.stream().map(ResearchPaper::getCreatedBy)
                .collect(Collectors.toSet()));
        papers.forEach(item -> activities.add(new ActivityItem("PAPER",
                "登记项目论文", item.getTitle(), user(paperUsers, item.getCreatedBy()),
                item.getProjectId(), item.getUpdatedAt().toLocalDate())));

        List<ProjectFile> files = fileMapper.selectList(Wrappers.<ProjectFile>lambdaQuery()
                .in(ProjectFile::getProjectId, projectIds)
                .orderByDesc(ProjectFile::getCreatedAt)
                .last("LIMIT 5"));
        Map<Long, User> fileUsers = loadUsers(files.stream().map(ProjectFile::getUploaderId)
                .collect(Collectors.toSet()));
        files.forEach(item -> activities.add(new ActivityItem("FILE",
                "上传项目文件", item.getOriginalName(), user(fileUsers, item.getUploaderId()),
                item.getProjectId(), item.getCreatedAt().toLocalDate())));

        return activities.stream()
                .sorted(Comparator.comparing(ActivityItem::date).reversed())
                .limit(10)
                .toList();
    }

    private List<ChartItem> taskDistribution(List<ResearchTask> tasks) {
        Map<String, Long> counts = tasks.stream().collect(Collectors.groupingBy(
                ResearchTask::getStatus, Collectors.counting()));
        return List.of(
                new ChartItem("待处理", counts.getOrDefault("TODO", 0L)),
                new ChartItem("进行中", counts.getOrDefault("IN_PROGRESS", 0L)),
                new ChartItem("已完成", counts.getOrDefault("DONE", 0L)),
                new ChartItem("已阻塞", counts.getOrDefault("BLOCKED", 0L)));
    }

    private List<ChartItem> projectProgress(List<ResearchProject> projects) {
        return projects.stream().limit(6)
                .map(project -> new ChartItem(project.getName(), project.getProgress()))
                .toList();
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        Set<Long> validIds = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (validIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(validIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private String user(Map<Long, User> users, Long id) {
        User user = users.get(id);
        return user == null ? "未知用户" : user.getRealName();
    }

    private String shorten(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 56 ? value.substring(0, 56) + "..." : value;
    }
}
