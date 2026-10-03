package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.BusinessException;
import com.campus.research.common.PageResult;
import com.campus.research.dto.DiscussionDtos.DiscussionRequest;
import com.campus.research.dto.DiscussionDtos.DiscussionView;
import com.campus.research.entity.ProjectDiscussion;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.User;
import com.campus.research.mapper.ProjectDiscussionMapper;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final ProjectDiscussionMapper discussionMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final ProjectAccessService accessService;
    private final NotificationService notificationService;

    public PageResult<DiscussionView> list(Long projectId, long page, long size) {
        accessService.requireMember(projectId);
        Page<ProjectDiscussion> result = discussionMapper.selectPage(new Page<>(page, size),
                Wrappers.<ProjectDiscussion>lambdaQuery()
                        .eq(ProjectDiscussion::getProjectId, projectId)
                        .isNull(ProjectDiscussion::getParentId)
                        .orderByDesc(ProjectDiscussion::getCreatedAt));
        Map<Long, User> users = loadUsers(result.getRecords().stream()
                .map(ProjectDiscussion::getAuthorId).collect(Collectors.toSet()));
        List<DiscussionView> views = new ArrayList<>();
        for (ProjectDiscussion discussion : result.getRecords()) {
            List<ProjectDiscussion> replies = discussionMapper.selectList(Wrappers.<ProjectDiscussion>lambdaQuery()
                    .eq(ProjectDiscussion::getParentId, discussion.getId())
                    .orderByAsc(ProjectDiscussion::getCreatedAt));
            users.putAll(loadUsers(replies.stream().map(ProjectDiscussion::getAuthorId)
                    .collect(Collectors.toSet())));
            views.add(toView(discussion, users, replies));
        }
        return new PageResult<>(views, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Transactional
    public DiscussionView create(DiscussionRequest request) {
        accessService.requireMember(request.projectId());
        if (request.parentId() != null) {
            ProjectDiscussion parent = discussionMapper.selectById(request.parentId());
            if (parent == null || !request.projectId().equals(parent.getProjectId())) {
                throw new BusinessException("回复的讨论不存在");
            }
        }
        ProjectDiscussion discussion = new ProjectDiscussion();
        discussion.setProjectId(request.projectId());
        discussion.setAuthorId(SecurityUtils.currentUserId());
        discussion.setParentId(request.parentId());
        discussion.setContent(request.content());
        discussionMapper.insert(discussion);
        notifyMembers(discussion);
        return toView(discussion, loadUsers(Set.of(discussion.getAuthorId())), List.of());
    }

    @Transactional
    public DiscussionView update(Long id, String content) {
        ProjectDiscussion discussion = requireDiscussion(id);
        if (!discussion.getAuthorId().equals(SecurityUtils.currentUserId())) {
            accessService.requireManager(discussion.getProjectId());
        } else {
            accessService.requireMember(discussion.getProjectId());
        }
        discussion.setContent(content);
        discussionMapper.updateById(discussion);
        return toView(discussion, loadUsers(Set.of(discussion.getAuthorId())), List.of());
    }

    @Transactional
    public void delete(Long id) {
        ProjectDiscussion discussion = requireDiscussion(id);
        if (!discussion.getAuthorId().equals(SecurityUtils.currentUserId())) {
            accessService.requireManager(discussion.getProjectId());
        } else {
            accessService.requireMember(discussion.getProjectId());
        }
        discussionMapper.delete(Wrappers.<ProjectDiscussion>lambdaQuery()
                .eq(ProjectDiscussion::getId, id)
                .or().eq(ProjectDiscussion::getParentId, id));
    }

    private void notifyMembers(ProjectDiscussion discussion) {
        List<ProjectMember> members = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, discussion.getProjectId()));
        String content = discussion.getContent().length() > 80
                ? discussion.getContent().substring(0, 80) + "..."
                : discussion.getContent();
        members.forEach(member -> notificationService.send(member.getUserId(), SecurityUtils.currentUserId(),
                discussion.getParentId() == null ? "项目新增讨论" : "讨论收到回复",
                content, "DISCUSSION", discussion.getProjectId(),
                "/projects/" + discussion.getProjectId()));
    }

    private ProjectDiscussion requireDiscussion(Long id) {
        ProjectDiscussion discussion = discussionMapper.selectById(id);
        if (discussion == null) {
            throw new BusinessException(404, "讨论不存在");
        }
        return discussion;
    }

    private DiscussionView toView(ProjectDiscussion discussion, Map<Long, User> users,
                                  List<ProjectDiscussion> replies) {
        User author = users.get(discussion.getAuthorId());
        List<DiscussionView> replyViews = replies.stream()
                .map(reply -> {
                    User replyAuthor = users.get(reply.getAuthorId());
                    return new DiscussionView(reply.getId(), reply.getProjectId(), reply.getAuthorId(),
                            replyAuthor == null ? "未知用户" : replyAuthor.getRealName(),
                            replyAuthor == null ? null : replyAuthor.getAvatar(),
                            reply.getParentId(), reply.getContent(), reply.getCreatedAt(),
                            reply.getUpdatedAt(), List.of());
                })
                .toList();
        return new DiscussionView(discussion.getId(), discussion.getProjectId(), discussion.getAuthorId(),
                author == null ? "未知用户" : author.getRealName(),
                author == null ? null : author.getAvatar(), discussion.getParentId(),
                discussion.getContent(), discussion.getCreatedAt(), discussion.getUpdatedAt(), replyViews);
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        Set<Long> validIds = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (validIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        return userMapper.selectBatchIds(validIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }
}
