package com.campus.research.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class DiscussionDtos {

    private DiscussionDtos() {
    }

    public record DiscussionRequest(
            @NotNull(message = "请选择所属项目") Long projectId,
            Long parentId,
            @NotBlank(message = "讨论内容不能为空") String content) {
    }

    public record DiscussionView(
            Long id,
            Long projectId,
            Long authorId,
            String authorName,
            String authorAvatar,
            Long parentId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<DiscussionView> replies) {
    }
}
