package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.BusinessException;
import com.campus.research.common.PageResult;
import com.campus.research.dto.TaskDtos.TaskRequest;
import com.campus.research.dto.TaskDtos.TaskView;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchProject;
import com.campus.research.entity.ResearchTask;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchProjectMapper;
import com.campus.research.mapper.ResearchTaskMapper;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final ResearchTaskMapper taskMapper;
    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final ProjectAccessService accessService;
    private final MappingService mappingService;
    private final NotificationService notificationService;

    public PageResult<TaskView> list(Long projectId, String status, String priority, Long assigneeId,
                                     Boolean mine, long page, long size) {
        Set<Long> accessible = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getUserId, SecurityUtils.currentUserId()))
                .stream().map(ProjectMember::getProjectId).collect(Collectors.toSet());
        if (!SecurityUtils.isAdmin()) {
            if (projectId != null) {
                accessService.requireMember(projectId);
            } else if (accessible.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size, 0);
            }
        }

        Long effectiveAssignee = Boolean.TRUE.equals(mine) ? SecurityUtils.currentUserId() : assigneeId;
        if (!SecurityUtils.isAdmin() && projectId != null && !isProjectManager(projectId)) {
            effectiveAssignee = SecurityUtils.currentUserId();
        }

        Set<Long> managerProjects = projectMapper.selectList(
                        Wrappers.<ResearchProject>lambdaQuery().in(!accessible.isEmpty(), ResearchProject::getId, accessible))
                .stream()
                .filter(project -> project.getLeaderId().equals(SecurityUtils.currentUserId())
                        || memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, project.getId())
                        .eq(ProjectMember::getUserId, SecurityUtils.currentUserId())
                        .in(ProjectMember::getProjectRole, "LEADER", "MANAGER")) > 0)
                .map(ResearchProject::getId)
                .collect(Collectors.toSet());

        var query = Wrappers.<ResearchTask>lambdaQuery()
                .eq(projectId != null, ResearchTask::getProjectId, projectId)
                .in(!SecurityUtils.isAdmin() && projectId == null, ResearchTask::getProjectId, accessible)
                .eq(StringUtils.hasText(status), ResearchTask::getStatus, status)
                .eq(StringUtils.hasText(priority), ResearchTask::getPriority, priority);
        if (effectiveAssignee != null) {
            query.eq(ResearchTask::getAssigneeId, effectiveAssignee);
        } else if (!SecurityUtils.isAdmin() && projectId == null && !managerProjects.isEmpty()) {
            query.and(wrapper -> wrapper.in(ResearchTask::getProjectId, managerProjects)
                    .or(or -> or.notIn(ResearchTask::getProjectId, managerProjects)
                            .eq(ResearchTask::getAssigneeId, SecurityUtils.currentUserId())));
        } else if (!SecurityUtils.isAdmin() && projectId == null) {
            query.eq(ResearchTask::getAssigneeId, SecurityUtils.currentUserId());
        }
        Page<ResearchTask> result = taskMapper.selectPage(new Page<>(page, size),
                query.orderByAsc(ResearchTask::getStatus)
                        .orderByAsc(ResearchTask::getDeadline)
                        .orderByDesc(ResearchTask::getUpdatedAt));
        return new PageResult<>(mappingService.toTaskViews(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    public TaskView get(Long id) {
        ResearchTask task = requireTask(id);
        accessService.requireMember(task.getProjectId());
        if (!SecurityUtils.isAdmin() && !isProjectManager(task.getProjectId())
                && !SecurityUtils.currentUserId().equals(task.getAssigneeId())) {
            throw new BusinessException(403, "只能查看分配给自己的任务");
        }
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public TaskView create(TaskRequest request) {
        accessService.requireManager(request.projectId());
        validateAssignee(request.projectId(), request.assigneeId());
        ResearchTask task = new ResearchTask();
        apply(task, request);
        task.setCreatorId(SecurityUtils.currentUserId());
        normalize(task);
        taskMapper.insert(task);
        if (task.getAssigneeId() != null) {
            ResearchProject project = accessService.requireProject(task.getProjectId());
            notificationService.send(task.getAssigneeId(), SecurityUtils.currentUserId(), "新任务已分配",
                    "您被分配负责“" + task.getTitle() + "”（项目：" + project.getName() + "）。",
                    "TASK", task.getId(), "/tasks");
        }
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public TaskView update(Long id, TaskRequest request) {
        ResearchTask task = requireTask(id);
        accessService.requireManager(task.getProjectId());
        if (!task.getProjectId().equals(request.projectId())) {
            throw new BusinessException("任务所属项目不可变更");
        }
        Long oldAssigneeId = task.getAssigneeId();
        apply(task, request);
        normalize(task);
        taskMapper.updateById(task);
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(oldAssigneeId)) {
            notificationService.send(task.getAssigneeId(), SecurityUtils.currentUserId(), "新任务已分配",
                    "您被分配负责“" + task.getTitle() + "”。", "TASK", task.getId(), "/tasks");
        }
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public TaskView updateStatus(Long id, String status) {
        ResearchTask task = requireTask(id);
        accessService.requireMember(task.getProjectId());
        if (!SecurityUtils.currentUserId().equals(task.getAssigneeId())
                && !SecurityUtils.currentUserId().equals(task.getCreatorId())) {
            accessService.requireManager(task.getProjectId());
        }
        if (StringUtils.hasText(status)) {
            task.setStatus(status);
        }
        taskMapper.updateById(task);
        if (task.getCreatorId() != null && !task.getCreatorId().equals(SecurityUtils.currentUserId())) {
            notificationService.send(task.getCreatorId(), SecurityUtils.currentUserId(), "任务状态已更新",
                    "“" + task.getTitle() + "”的状态已更新为 " + task.getStatus() + "。",
                    "TASK", task.getId(), "/tasks");
        }
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        ResearchTask task = requireTask(id);
        accessService.requireManager(task.getProjectId());
        taskMapper.deleteById(id);
    }

    private ResearchTask requireTask(Long id) {
        ResearchTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private void validateAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        long count = memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, assigneeId));
        if (count == 0) {
            throw new BusinessException("负责人必须是项目成员");
        }
    }

    private void apply(ResearchTask task, TaskRequest request) {
        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        validateAssignee(request.projectId(), request.assigneeId());
        task.setAssigneeId(request.assigneeId());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setDeadline(request.deadline());
    }

    private void normalize(ResearchTask task) {
        if (!StringUtils.hasText(task.getPriority())) {
            task.setPriority("MEDIUM");
        }
        if (!StringUtils.hasText(task.getStatus())) {
            task.setStatus("TODO");
        }
    }

    private boolean isProjectManager(Long projectId) {
        ResearchProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        if (SecurityUtils.isAdmin() || project.getLeaderId().equals(SecurityUtils.currentUserId())) {
            return true;
        }
        return memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, SecurityUtils.currentUserId())
                .in(ProjectMember::getProjectRole, "LEADER", "MANAGER")) > 0;
    }
}
