package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.research.dto.PaperDtos.PaperView;
import com.campus.research.dto.ProjectDtos.MemberView;
import com.campus.research.dto.ProjectDtos.ProjectView;
import com.campus.research.dto.TaskDtos.TaskView;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchPaper;
import com.campus.research.entity.ResearchProject;
import com.campus.research.entity.ResearchTask;
import com.campus.research.entity.User;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchPaperMapper;
import com.campus.research.mapper.ResearchProjectMapper;
import com.campus.research.mapper.ResearchTaskMapper;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MappingService {

    private final UserMapper userMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchTaskMapper taskMapper;
    private final ResearchPaperMapper paperMapper;
    private final ResearchProjectMapper projectMapper;

    public List<ProjectView> toProjectViews(List<ResearchProject> projects) {
        if (projects.isEmpty()) {
            return List.of();
        }
        Set<Long> leaderIds = projects.stream().map(ResearchProject::getLeaderId).collect(Collectors.toSet());
        Set<Long> projectIds = projects.stream().map(ResearchProject::getId).collect(Collectors.toSet());
        Map<Long, User> users = mapUsers(leaderIds);
        List<ProjectMember> allMembers = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .in(ProjectMember::getProjectId, projectIds));
        Map<Long, List<ProjectMember>> membersByProject = allMembers.stream()
                .collect(Collectors.groupingBy(ProjectMember::getProjectId));
        List<ResearchTask> allTasks = taskMapper.selectList(Wrappers.<ResearchTask>lambdaQuery()
                .in(ResearchTask::getProjectId, projectIds));
        Map<Long, List<ResearchTask>> tasksByProject = allTasks.stream()
                .collect(Collectors.groupingBy(ResearchTask::getProjectId));
        Long currentUserId = SecurityUtils.currentUserId();

        return projects.stream().map(project -> {
            List<ProjectMember> members = membersByProject.getOrDefault(project.getId(), List.of());
            List<ResearchTask> tasks = tasksByProject.getOrDefault(project.getId(), List.of());
            String currentRole = members.stream()
                    .filter(member -> member.getUserId().equals(currentUserId))
                    .map(ProjectMember::getProjectRole)
                    .findFirst().orElse(SecurityUtils.isAdmin() ? "ADMIN" : null);
            return new ProjectView(project.getId(), project.getName(), project.getCode(),
                    project.getDescription(), project.getCategory(), project.getSource(),
                    project.getLeaderId(),
                    users.containsKey(project.getLeaderId()) ? users.get(project.getLeaderId()).getRealName() : "未知",
                    project.getStatus(), project.getProgress(), project.getStartDate(), project.getEndDate(),
                    project.getBudget(), (long) members.size(), (long) tasks.size(),
                    tasks.stream().filter(task -> "DONE".equals(task.getStatus())).count(),
                    currentRole, project.getUpdatedAt());
        }).toList();
    }

    public List<TaskView> toTaskViews(List<ResearchTask> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        Set<Long> projectIds = tasks.stream().map(ResearchTask::getProjectId).collect(Collectors.toSet());
        Set<Long> userIds = tasks.stream().flatMap(task -> java.util.stream.Stream.of(
                task.getAssigneeId(), task.getCreatorId())).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ResearchProject> projects = mapProjects(projectIds);
        Map<Long, User> users = mapUsers(userIds);
        return tasks.stream().map(task -> {
            ResearchProject project = projects.get(task.getProjectId());
            User assignee = users.get(task.getAssigneeId());
            User creator = users.get(task.getCreatorId());
            return new TaskView(task.getId(), task.getProjectId(),
                    project == null ? "未知项目" : project.getName(), task.getTitle(), task.getDescription(),
                    task.getAssigneeId(), assignee == null ? "待认领" : assignee.getRealName(),
                    assignee == null ? null : assignee.getAvatar(), task.getCreatorId(),
                    creator == null ? "未知" : creator.getRealName(), task.getPriority(), task.getStatus(),
                    task.getProgress(), task.getDeadline(), task.getCreatedAt(), task.getUpdatedAt());
        }).toList();
    }

    public List<PaperView> toPaperViews(List<ResearchPaper> papers) {
        if (papers.isEmpty()) {
            return List.of();
        }
        Map<Long, ResearchProject> projects = mapProjects(
                papers.stream().map(ResearchPaper::getProjectId).collect(Collectors.toSet()));
        Map<Long, User> users = mapUsers(
                papers.stream().map(ResearchPaper::getCreatedBy).collect(Collectors.toSet()));
        return papers.stream().map(paper -> new PaperView(paper.getId(), paper.getProjectId(),
                projects.containsKey(paper.getProjectId()) ? projects.get(paper.getProjectId()).getName() : "未知项目",
                paper.getTitle(), paper.getAuthors(), paper.getJournal(), paper.getPublicationDate(),
                paper.getDoi(), paper.getUrl(), paper.getStatus(), paper.getAbstractText(),
                paper.getCreatedBy(), users.containsKey(paper.getCreatedBy())
                        ? users.get(paper.getCreatedBy()).getRealName() : "未知",
                paper.getUpdatedAt())).toList();
    }

    public Map<Long, User> mapUsers(Set<Long> ids) {
        Set<Long> validIds = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(validIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public Map<Long, ResearchProject> mapProjects(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return projectMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ResearchProject::getId, Function.identity()));
    }
}
