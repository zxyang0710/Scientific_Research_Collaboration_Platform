package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.common.PageResult;
import com.campus.research.service.FileService;
import com.campus.research.service.FileService.DownloadFile;
import com.campus.research.service.FileService.FileView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    public ApiResponse<PageResult<FileView>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(fileService.list(projectId, category, keyword, page, size));
    }

    @PostMapping("/upload")
    public ApiResponse<FileView> upload(@RequestParam Long projectId,
                                        @RequestParam MultipartFile file,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String description) {
        return ApiResponse.success("文件上传成功", fileService.upload(projectId, file, category, description));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        DownloadFile download = fileService.download(id);
        return fileResponse(download, true);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<org.springframework.core.io.Resource> view(@PathVariable Long id) {
        DownloadFile download = fileService.download(id);
        return fileResponse(download, false);
    }

    private ResponseEntity<org.springframework.core.io.Resource> fileResponse(DownloadFile download, boolean attachment) {
        String encoded = java.net.URLEncoder.encode(download.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String detectedType = download.resource().getURL().openConnection().getContentType();
            if (detectedType != null) {
                contentType = MediaType.parseMediaType(detectedType);
            }
        } catch (Exception ignored) {
            // Use binary content when the file type cannot be detected.
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (attachment ? "attachment" : "inline") + "; filename*=UTF-8''" + encoded)
                .contentType(contentType)
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.success();
    }
}
