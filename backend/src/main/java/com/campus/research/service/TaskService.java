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
import com.campus.research.mapper.ResearchTaskMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final ResearchTaskMapper taskMapper;
    private final ProjectMemberMapper memberMapper;
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
        Page<ResearchTask> result = taskMapper.selectPage(new Page<>(page, size),
                Wrappers.<ResearchTask>lambdaQuery()
                        .eq(projectId != null, ResearchTask::getProjectId, projectId)
                        .in(!SecurityUtils.isAdmin() && projectId == null, ResearchTask::getProjectId, accessible)
                        .eq(StringUtils.hasText(status), ResearchTask::getStatus, status)
                        .eq(StringUtils.hasText(priority), ResearchTask::getPriority, priority)
                        .eq(effectiveAssignee != null, ResearchTask::getAssigneeId, effectiveAssignee)
                        .orderByAsc(ResearchTask::getStatus)
                        .orderByAsc(ResearchTask::getDeadline)
                        .orderByDesc(ResearchTask::getUpdatedAt));
        return new PageResult<>(mappingService.toTaskViews(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    public TaskView get(Long id) {
        ResearchTask task = requireTask(id);
        accessService.requireMember(task.getProjectId());
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public TaskView create(TaskRequest request) {
        accessService.requireMember(request.projectId());
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
        accessService.requireCreatorOrManager(task.getProjectId(), task.getCreatorId());
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
    public TaskView updateProgress(Long id, Integer progress, String status) {
        ResearchTask task = requireTask(id);
        accessService.requireMember(task.getProjectId());
        if (!SecurityUtils.currentUserId().equals(task.getAssigneeId())
                && !SecurityUtils.currentUserId().equals(task.getCreatorId())) {
            accessService.requireManager(task.getProjectId());
        }
        if (progress != null) {
            task.setProgress(Math.max(0, Math.min(100, progress)));
            if (task.getProgress() == 100 && !StringUtils.hasText(status)) {
                task.setStatus("DONE");
            }
        }
        if (StringUtils.hasText(status)) {
            task.setStatus(status);
            if ("DONE".equals(status)) {
                task.setProgress(100);
            }
        }
        taskMapper.updateById(task);
        if (task.getCreatorId() != null && !task.getCreatorId().equals(SecurityUtils.currentUserId())) {
            notificationService.send(task.getCreatorId(), SecurityUtils.currentUserId(), "任务进度已更新",
                    "“" + task.getTitle() + "”的进度更新为 " + task.getProgress() + "%。",
                    "TASK", task.getId(), "/tasks");
        }
        return mappingService.toTaskViews(List.of(task)).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        ResearchTask task = requireTask(id);
        accessService.requireCreatorOrManager(task.getProjectId(), task.getCreatorId());
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
        task.setProgress(request.progress());
        task.setDeadline(request.deadline());
    }

    private void normalize(ResearchTask task) {
        if (!StringUtils.hasText(task.getPriority())) {
            task.setPriority("MEDIUM");
        }
        if (!StringUtils.hasText(task.getStatus())) {
            task.setStatus("TODO");
        }
        if (task.getProgress() == null) {
            task.setProgress("DONE".equals(task.getStatus()) ? 100 : 0);
        } else {
            task.setProgress(Math.max(0, Math.min(100, task.getProgress())));
        }
    }
}
