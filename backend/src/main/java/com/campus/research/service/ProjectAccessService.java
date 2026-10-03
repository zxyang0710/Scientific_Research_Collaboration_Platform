package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.research.common.BusinessException;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchProject;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchProjectMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    public ResearchProject requireProject(Long projectId) {
        ResearchProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在");
        }
        return project;
    }

    public void requireMember(Long projectId) {
        requireProject(projectId);
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long count = memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, SecurityUtils.currentUserId()));
        if (count == 0) {
            throw new BusinessException(403, "您不是该项目成员");
        }
    }

    public void requireManager(Long projectId) {
        ResearchProject project = requireProject(projectId);
        if (SecurityUtils.isAdmin() || project.getLeaderId().equals(SecurityUtils.currentUserId())) {
            return;
        }
        ProjectMember member = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, SecurityUtils.currentUserId()));
        if (member == null || (!"LEADER".equals(member.getProjectRole())
                && !"MANAGER".equals(member.getProjectRole()))) {
            throw new BusinessException(403, "仅项目负责人可执行此操作");
        }
    }

    public void requireCreatorOrManager(Long projectId, Long creatorId) {
        if (SecurityUtils.currentUserId().equals(creatorId)) {
            requireMember(projectId);
        } else {
            requireManager(projectId);
        }
    }
}
