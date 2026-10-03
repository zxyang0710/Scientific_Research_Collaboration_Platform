package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.BusinessException;
import com.campus.research.common.PageResult;
import com.campus.research.entity.ProjectFile;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.User;
import com.campus.research.mapper.ProjectFileMapper;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.UserMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ProjectFileMapper fileMapper;
    private final ProjectMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final ProjectAccessService accessService;
    private final NotificationService notificationService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public PageResult<FileView> list(Long projectId, String category, String keyword, long page, long size) {
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
        Page<ProjectFile> result = fileMapper.selectPage(new Page<>(page, size),
                Wrappers.<ProjectFile>lambdaQuery()
                        .eq(projectId != null, ProjectFile::getProjectId, projectId)
                        .in(!SecurityUtils.isAdmin() && projectId == null, ProjectFile::getProjectId, accessible)
                        .eq(StringUtils.hasText(category), ProjectFile::getCategory, category)
                        .like(StringUtils.hasText(keyword), ProjectFile::getOriginalName, keyword)
                        .orderByDesc(ProjectFile::getCreatedAt));
        Set<Long> uploaderIds = result.getRecords().stream()
                .map(ProjectFile::getUploaderId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = uploaderIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<FileView> views = result.getRecords().stream().map(file -> {
            User uploader = users.get(file.getUploaderId());
            return new FileView(file.getId(), file.getProjectId(), file.getOriginalName(),
                    file.getFileSize(), file.getContentType(), file.getCategory(), file.getDescription(),
                    file.getUploaderId(), uploader == null ? "未知用户" : uploader.getRealName(),
                    file.getCreatedAt());
        }).toList();
        return new PageResult<>(views, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    @Transactional
    public FileView upload(Long projectId, MultipartFile multipartFile, String category,
                           String description) {
        accessService.requireMember(projectId);
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String originalName = StringUtils.cleanPath(
                multipartFile.getOriginalFilename() == null ? "未命名文件" : multipartFile.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BusinessException("文件名不合法");
        }
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "-" + originalName;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path directory = root.resolve(datePath).normalize();
        if (!directory.startsWith(root)) {
            throw new BusinessException("上传路径不合法");
        }
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("上传路径不合法");
            }
            Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            ProjectFile file = new ProjectFile();
            file.setProjectId(projectId);
            file.setUploaderId(SecurityUtils.currentUserId());
            file.setOriginalName(originalName);
            file.setStoredName(storedName);
            file.setFilePath(root.relativize(target).toString().replace('\\', '/'));
            file.setFileSize(multipartFile.getSize());
            file.setContentType(multipartFile.getContentType());
            file.setCategory(StringUtils.hasText(category) ? category : "DOCUMENT");
            file.setDescription(description);
            file.setCreatedAt(LocalDateTime.now());
            file.setDeleted(0);
            fileMapper.insert(file);
            notifyMembers(projectId, originalName);
            return list(projectId, null, null, 1, 100).getRecords().stream()
                    .filter(item -> item.id().equals(file.getId()))
                    .findFirst().orElse(toView(file));
        } catch (IOException exception) {
            throw new BusinessException("文件上传失败：" + exception.getMessage());
        }
    }

    public DownloadFile download(Long id) {
        ProjectFile file = requireFile(id);
        accessService.requireMember(file.getProjectId());
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(file.getFilePath()).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("文件路径不合法");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(404, "文件不存在或已损坏");
            }
            return new DownloadFile(file.getOriginalName(), resource);
        } catch (MalformedURLException exception) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    @Transactional
    public void delete(Long id) {
        ProjectFile file = requireFile(id);
        accessService.requireCreatorOrManager(file.getProjectId(), file.getUploaderId());
        fileMapper.deleteById(id);
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(file.getFilePath()).normalize();
        if (path.startsWith(root)) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Metadata deletion is authoritative; a leftover file can be cleaned by an administrator.
            }
        }
    }

    private ProjectFile requireFile(Long id) {
        ProjectFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(404, "文件不存在");
        }
        return file;
    }

    private void notifyMembers(Long projectId, String fileName) {
        memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId))
                .forEach(member -> notificationService.send(member.getUserId(), SecurityUtils.currentUserId(),
                        "项目文件已更新", "项目新增文件“" + fileName + "”。",
                        "FILE", projectId, "/files"));
    }

    private FileView toView(ProjectFile file) {
        User uploader = userMapper.selectById(file.getUploaderId());
        return new FileView(file.getId(), file.getProjectId(), file.getOriginalName(), file.getFileSize(),
                file.getContentType(), file.getCategory(), file.getDescription(), file.getUploaderId(),
                uploader == null ? "未知用户" : uploader.getRealName(), file.getCreatedAt());
    }

    public record FileView(
            Long id,
            Long projectId,
            String originalName,
            Long fileSize,
            String contentType,
            String category,
            String description,
            Long uploaderId,
            String uploaderName,
            LocalDateTime createdAt) {
    }

    public record DownloadFile(String filename, Resource resource) {
    }
}
