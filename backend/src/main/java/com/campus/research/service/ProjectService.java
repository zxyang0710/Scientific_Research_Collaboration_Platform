package com.campus.research.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.BusinessException;
import com.campus.research.common.PageResult;
import com.campus.research.dto.ProjectDtos.MemberRequest;
import com.campus.research.dto.ProjectDtos.MemberView;
import com.campus.research.dto.ProjectDtos.ProjectDetail;
import com.campus.research.dto.ProjectDtos.ProjectRequest;
import com.campus.research.dto.ProjectDtos.ProjectView;
import com.campus.research.dto.PaperDtos.PaperView;
import com.campus.research.dto.TaskDtos.TaskView;
import com.campus.research.entity.ProjectDiscussion;
import com.campus.research.entity.ProjectFile;
import com.campus.research.entity.ProjectMember;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchTaskMapper taskMapper;
    private final ProjectFileMapper fileMapper;
    private final ProjectDiscussionMapper discussionMapper;
    private final ResearchPaperMapper paperMapper;
    private final UserMapper userMapper;
    private final ProjectAccessService accessService;
    private final MappingService mappingService;
    private final NotificationService notificationService;

    public PageResult<ProjectView> list(String keyword, String status, long page, long size) {
        Long userId = SecurityUtils.currentUserId();
        Set<Long> projectIds = accessibleProjectIds(userId);
        if (projectIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size, 0);
        }
        LambdaQueryWrapper<ResearchProject> wrapper = Wrappers.<ResearchProject>lambdaQuery()
                .in(ResearchProject::getId, projectIds)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(ResearchProject::getName, keyword)
                        .or().like(ResearchProject::getCode, keyword))
                .eq(StringUtils.hasText(status), ResearchProject::getStatus, status)
                .orderByDesc(ResearchProject::getUpdatedAt);
        Page<ResearchProject> result = projectMapper.selectPage(new Page<>(page, size), wrapper);
        List<ProjectView> views = mappingService.toProjectViews(result.getRecords());
        return new PageResult<>(views, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    public ProjectView get(Long id) {
        accessService.requireMember(id);
        return mappingService.toProjectViews(List.of(accessService.requireProject(id))).getFirst();
    }

    public ProjectDetail detail(Long id) {
        accessService.requireMember(id);
        ResearchProject project = accessService.requireProject(id);
        ProjectView projectView = mappingService.toProjectViews(List.of(project)).getFirst();
        List<MemberView> members = members(id);
        var taskQuery = Wrappers.<ResearchTask>lambdaQuery()
                .eq(ResearchTask::getProjectId, id)
                .orderByAsc(ResearchTask::getStatus)
                .orderByDesc(ResearchTask::getUpdatedAt);
        User currentUser = userMapper.selectById(SecurityUtils.currentUserId());
        if (!SecurityUtils.isAdmin() && currentUser != null && "学生".equals(currentUser.getTitle())
                && !isProjectManager(id)) {
            taskQuery.eq(ResearchTask::getAssigneeId, SecurityUtils.currentUserId());
        }
        List<TaskView> tasks = mappingService.toTaskViews(taskMapper.selectList(taskQuery));
        List<PaperView> papers = mappingService.toPaperViews(
                paperMapper.selectList(Wrappers.<com.campus.research.entity.ResearchPaper>lambdaQuery()
                        .eq(com.campus.research.entity.ResearchPaper::getProjectId, id)
                        .orderByDesc(com.campus.research.entity.ResearchPaper::getUpdatedAt)));
        long fileCount = fileMapper.selectCount(Wrappers.<ProjectFile>lambdaQuery()
                .eq(ProjectFile::getProjectId, id));
        long discussionCount = discussionMapper.selectCount(Wrappers.<ProjectDiscussion>lambdaQuery()
                .eq(ProjectDiscussion::getProjectId, id));
        return new ProjectDetail(projectView, members, tasks, papers, fileCount, discussionCount);
    }

    private boolean isProjectManager(Long projectId) {
        ResearchProject project = accessService.requireProject(projectId);
        if (project.getLeaderId().equals(SecurityUtils.currentUserId()) || SecurityUtils.isAdmin()) {
            return true;
        }
        ProjectMember member = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, SecurityUtils.currentUserId()));
        return member != null && ("LEADER".equals(member.getProjectRole())
                || "MANAGER".equals(member.getProjectRole()));
    }

    @Transactional
    public ProjectView create(ProjectRequest request) {
        long duplicate = projectMapper.selectCount(Wrappers.<ResearchProject>lambdaQuery()
                .eq(ResearchProject::getCode, request.code()));
        if (duplicate > 0) {
            throw new BusinessException("项目编号已存在");
        }
        Long leaderId = request.leaderId() == null ? SecurityUtils.currentUserId() : request.leaderId();
        User leader = userMapper.selectById(leaderId);
        if (leader == null) {
            throw new BusinessException("项目负责人不存在");
        }
        ResearchProject project = new ResearchProject();
        apply(project, request);
        project.setLeaderId(leaderId);
        project.setStatus(StringUtils.hasText(request.status()) ? request.status() : "PLANNING");
        project.setProgress(request.progress() == null ? 0 : request.progress());
        projectMapper.insert(project);
        addMember(project.getId(), leaderId, "LEADER", false);
        if (!leaderId.equals(SecurityUtils.currentUserId())) {
            addMember(project.getId(), SecurityUtils.currentUserId(), "MANAGER", false);
        }
        return mappingService.toProjectViews(List.of(project)).getFirst();
    }

    @Transactional
    public ProjectView update(Long id, ProjectRequest request) {
        accessService.requireManager(id);
        ResearchProject project = accessService.requireProject(id);
        long duplicate = projectMapper.selectCount(Wrappers.<ResearchProject>lambdaQuery()
                .eq(ResearchProject::getCode, request.code())
                .ne(ResearchProject::getId, id));
        if (duplicate > 0) {
            throw new BusinessException("项目编号已存在");
        }
        Long oldLeaderId = project.getLeaderId();
        apply(project, request);
        if (request.leaderId() != null) {
            User leader = userMapper.selectById(request.leaderId());
            if (leader == null) {
                throw new BusinessException("项目负责人不存在");
            }
            project.setLeaderId(request.leaderId());
        }
        projectMapper.updateById(project);
        if (request.leaderId() != null && !request.leaderId().equals(oldLeaderId)) {
            ProjectMember oldMember = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                    .eq(ProjectMember::getProjectId, id)
                    .eq(ProjectMember::getUserId, request.leaderId()));
            if (oldMember == null) {
                addMember(id, request.leaderId(), "LEADER", false);
            } else {
                oldMember.setProjectRole("LEADER");
                memberMapper.updateById(oldMember);
            }
            notificationService.send(request.leaderId(), id, "项目负责人变更",
                    "您已成为项目“" + project.getName() + "”的负责人。", "PROJECT", id,
                    "/projects/" + id);
        }
        return mappingService.toProjectViews(List.of(project)).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        accessService.requireManager(id);
        projectMapper.deleteById(id);
    }

    public List<MemberView> members(Long projectId) {
        accessService.requireMember(projectId);
        List<ProjectMember> members = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getJoinedAt));
        if (members.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = members.stream()
                .map(ProjectMember::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return members.stream().map(member -> {
            User user = users.get(member.getUserId());
            return new MemberView(member.getId(), member.getUserId(),
                    user == null ? "" : user.getUsername(),
                    user == null ? "未知用户" : user.getRealName(),
                    user == null ? "" : user.getDepartment(),
                    user == null ? "" : user.getTitle(),
                    user == null ? null : user.getAvatar(),
                    member.getProjectRole(), member.getJoinedAt());
        }).toList();
    }

    @Transactional
    public MemberView addMember(Long projectId, MemberRequest request) {
        accessService.requireManager(projectId);
        return addMember(projectId, request.userId(), request.projectRole(), true);
    }

    private MemberView addMember(Long projectId, Long userId, String role, boolean notify) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        ProjectMember existing = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (existing != null) {
            throw new BusinessException("该用户已是项目成员");
        }
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setProjectRole(role);
        member.setJoinedAt(java.time.LocalDateTime.now());
        memberMapper.insert(member);
        if (notify) {
            ResearchProject project = accessService.requireProject(projectId);
            notificationService.send(userId, SecurityUtils.currentUserId(), "加入科研项目",
                    "您已加入项目“" + project.getName() + "”。", "PROJECT", projectId,
                    "/projects/" + projectId);
        }
        return new MemberView(member.getId(), userId, user.getUsername(), user.getRealName(),
                user.getDepartment(), user.getTitle(), user.getAvatar(), role, member.getJoinedAt());
    }

    @Transactional
    public MemberView updateMember(Long projectId, Long memberId, MemberRequest request) {
        accessService.requireManager(projectId);
        ProjectMember member = memberMapper.selectById(memberId);
        if (member == null || !projectId.equals(member.getProjectId())) {
            throw new BusinessException(404, "项目成员不存在");
        }
        ResearchProject project = accessService.requireProject(projectId);
        if (member.getUserId().equals(project.getLeaderId()) && !"LEADER".equals(request.projectRole())) {
            throw new BusinessException("项目负责人角色不能直接变更，请先更换负责人");
        }
        member.setProjectRole(request.projectRole());
        memberMapper.updateById(member);
        return members(projectId).stream()
                .filter(item -> item.id().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("项目成员不存在"));
    }

    @Transactional
    public void removeMember(Long projectId, Long memberId) {
        accessService.requireManager(projectId);
        ProjectMember member = memberMapper.selectById(memberId);
        if (member == null || !projectId.equals(member.getProjectId())) {
            throw new BusinessException(404, "项目成员不存在");
        }
        ResearchProject project = accessService.requireProject(projectId);
        if (member.getUserId().equals(project.getLeaderId())) {
            throw new BusinessException("不能移除项目负责人");
        }
        memberMapper.deleteById(memberId);
    }

    private void apply(ResearchProject project, ProjectRequest request) {
        project.setName(request.name());
        project.setCode(request.code());
        project.setDescription(request.description());
        project.setCategory(request.category());
        project.setSource(request.source());
        project.setStatus(request.status());
        project.setProgress(request.progress());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setBudget(request.budget());
    }

    private Set<Long> accessibleProjectIds(Long userId) {
        if (SecurityUtils.isAdmin()) {
            return projectMapper.selectList(null).stream()
                    .map(ResearchProject::getId).collect(Collectors.toSet());
        }
        return memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getUserId, userId))
                .stream().map(ProjectMember::getProjectId).collect(Collectors.toSet());
    }
}
