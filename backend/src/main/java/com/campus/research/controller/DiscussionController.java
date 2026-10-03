package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.common.PageResult;
import com.campus.research.dto.DiscussionDtos.DiscussionRequest;
import com.campus.research.dto.DiscussionDtos.DiscussionView;
import com.campus.research.service.DiscussionService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @GetMapping
    public ApiResponse<PageResult<DiscussionView>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(discussionService.list(projectId, page, size));
    }

    @PostMapping
    public ApiResponse<DiscussionView> create(@Valid @RequestBody DiscussionRequest request) {
        return ApiResponse.success("讨论发布成功", discussionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DiscussionView> update(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        return ApiResponse.success("讨论更新成功", discussionService.update(id, body.get("content")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        discussionService.delete(id);
        return ApiResponse.success();
    }
}
