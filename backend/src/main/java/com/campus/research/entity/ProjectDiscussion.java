package com.campus.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_discussion")
public class ProjectDiscussion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long authorId;
    private Long parentId;
    private String content;
}
