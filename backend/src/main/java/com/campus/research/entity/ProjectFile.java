package com.campus.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_file")
public class ProjectFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long uploaderId;
    private String originalName;
    private String storedName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String category;
    private String description;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
