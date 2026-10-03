package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.common.PageResult;
import com.campus.research.dto.PaperDtos.PaperRequest;
import com.campus.research.dto.PaperDtos.PaperView;
import com.campus.research.service.PaperService;
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

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    public ApiResponse<PageResult<PaperView>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(paperService.list(projectId, status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaperView> get(@PathVariable Long id) {
        return ApiResponse.success(paperService.get(id));
    }

    @PostMapping
    public ApiResponse<PaperView> create(@Valid @RequestBody PaperRequest request) {
        return ApiResponse.success("论文登记成功", paperService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PaperView> update(@PathVariable Long id,
                                         @Valid @RequestBody PaperRequest request) {
        return ApiResponse.success("论文更新成功", paperService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return ApiResponse.success();
    }
}
