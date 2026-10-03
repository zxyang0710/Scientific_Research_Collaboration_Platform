package com.campus.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("research_project")
public class ResearchProject extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String description;
    private String category;
    private String source;
    private Long leaderId;
    private String status;
    private Integer progress;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
}
