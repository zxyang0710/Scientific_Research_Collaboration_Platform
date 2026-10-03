package com.campus.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("research_paper")
public class ResearchPaper extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String title;
    private String authors;
    private String journal;
    private LocalDate publicationDate;
    private String doi;
    private String url;
    private String status;
    private String abstractText;
    private Long createdBy;
}
