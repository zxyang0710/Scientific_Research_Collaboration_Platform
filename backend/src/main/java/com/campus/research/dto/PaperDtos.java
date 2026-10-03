package com.campus.research.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PaperDtos {

    private PaperDtos() {
    }

    public record PaperRequest(
            @NotNull(message = "请选择所属项目") Long projectId,
            @NotBlank(message = "论文标题不能为空")
            @Size(max = 250, message = "论文标题不能超过 250 个字符") String title,
            String authors,
            String journal,
            LocalDate publicationDate,
            String doi,
            String url,
            String status,
            String abstractText) {
    }

    public record PaperView(
            Long id,
            Long projectId,
            String projectName,
            String title,
            String authors,
            String journal,
            LocalDate publicationDate,
            String doi,
            String url,
            String status,
            String abstractText,
            Long createdBy,
            String creatorName,
            LocalDateTime updatedAt) {
    }
}
