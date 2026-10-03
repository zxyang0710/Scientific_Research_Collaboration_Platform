package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.common.PageResult;
import com.campus.research.dto.ProjectDtos.MemberRequest;
import com.campus.research.dto.ProjectDtos.MemberView;
import com.campus.research.dto.ProjectDtos.ProjectDetail;
import com.campus.research.dto.ProjectDtos.ProjectRequest;
import com.campus.research.dto.ProjectDtos.ProjectView;
import com.campus.research.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<PageResult<ProjectView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(projectService.list(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(projectService.detail(id));
    }

    @PostMapping
    public ApiResponse<ProjectView> create(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success("项目创建成功", projectService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectView> update(@PathVariable Long id,
                                           @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success("项目更新成功", projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<MemberView>> members(@PathVariable Long id) {
        return ApiResponse.success(projectService.members(id));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<MemberView> addMember(@PathVariable Long id,
                                             @Valid @RequestBody MemberRequest request) {
        return ApiResponse.success("成员添加成功", projectService.addMember(id, request));
    }

    @PutMapping("/{id}/members/{memberId}")
    public ApiResponse<MemberView> updateMember(@PathVariable Long id, @PathVariable Long memberId,
                                                @Valid @RequestBody MemberRequest request) {
        return ApiResponse.success("成员角色更新成功", projectService.updateMember(id, memberId, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        projectService.removeMember(id, memberId);
        return ApiResponse.success();
    }
}
