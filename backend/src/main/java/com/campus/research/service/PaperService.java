package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.BusinessException;
import com.campus.research.common.PageResult;
import com.campus.research.dto.PaperDtos.PaperRequest;
import com.campus.research.dto.PaperDtos.PaperView;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchPaper;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchPaperMapper;
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
public class PaperService {

    private final ResearchPaperMapper paperMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectAccessService accessService;
    private final MappingService mappingService;
    private final NotificationService notificationService;

    public PageResult<PaperView> list(Long projectId, String status, String keyword, long page, long size) {
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
        Page<ResearchPaper> result = paperMapper.selectPage(new Page<>(page, size),
                Wrappers.<ResearchPaper>lambdaQuery()
                        .eq(projectId != null, ResearchPaper::getProjectId, projectId)
                        .in(!SecurityUtils.isAdmin() && projectId == null, ResearchPaper::getProjectId, accessible)
                        .eq(StringUtils.hasText(status), ResearchPaper::getStatus, status)
                        .and(StringUtils.hasText(keyword), query -> query
                                .like(ResearchPaper::getTitle, keyword)
                                .or().like(ResearchPaper::getAuthors, keyword)
                                .or().like(ResearchPaper::getJournal, keyword))
                        .orderByDesc(ResearchPaper::getUpdatedAt));
        return new PageResult<>(mappingService.toPaperViews(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    public PaperView get(Long id) {
        ResearchPaper paper = requirePaper(id);
        accessService.requireMember(paper.getProjectId());
        return mappingService.toPaperViews(List.of(paper)).getFirst();
    }

    @Transactional
    public PaperView create(PaperRequest request) {
        accessService.requireMember(request.projectId());
        ResearchPaper paper = new ResearchPaper();
        apply(paper, request);
        paper.setCreatedBy(SecurityUtils.currentUserId());
        if (!StringUtils.hasText(paper.getStatus())) {
            paper.setStatus("DRAFT");
        }
        paperMapper.insert(paper);
        notifyProjectMembers(paper, "新增项目论文", "项目新增论文“" + paper.getTitle() + "”。");
        return mappingService.toPaperViews(List.of(paper)).getFirst();
    }

    @Transactional
    public PaperView update(Long id, PaperRequest request) {
        ResearchPaper paper = requirePaper(id);
        accessService.requireCreatorOrManager(paper.getProjectId(), paper.getCreatedBy());
        if (!paper.getProjectId().equals(request.projectId())) {
            throw new BusinessException("论文所属项目不可变更");
        }
        apply(paper, request);
        paperMapper.updateById(paper);
        return mappingService.toPaperViews(List.of(paper)).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        ResearchPaper paper = requirePaper(id);
        accessService.requireCreatorOrManager(paper.getProjectId(), paper.getCreatedBy());
        paperMapper.deleteById(id);
    }

    private void notifyProjectMembers(ResearchPaper paper, String title, String content) {
        List<ProjectMember> members = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, paper.getProjectId()));
        members.forEach(member -> notificationService.send(member.getUserId(), SecurityUtils.currentUserId(),
                title, content, "PAPER", paper.getId(), "/papers"));
    }

    private ResearchPaper requirePaper(Long id) {
        ResearchPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BusinessException(404, "论文记录不存在");
        }
        return paper;
    }

    private void apply(ResearchPaper paper, PaperRequest request) {
        paper.setProjectId(request.projectId());
        paper.setTitle(request.title());
        paper.setAuthors(request.authors());
        paper.setJournal(request.journal());
        paper.setPublicationDate(request.publicationDate());
        paper.setDoi(request.doi());
        paper.setUrl(request.url());
        paper.setStatus(request.status());
        paper.setAbstractText(request.abstractText());
    }
}
