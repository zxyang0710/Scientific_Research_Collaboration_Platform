package com.campus.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("research_task")
public class ResearchTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Long assigneeId;
    private Long creatorId;
    private String priority;
    private String status;
    private Integer progress;
    private LocalDate deadline;
}
