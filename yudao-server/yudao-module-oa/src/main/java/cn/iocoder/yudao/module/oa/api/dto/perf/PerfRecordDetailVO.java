package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PerfRecordDetailVO {

    private Long id;
    private Long targetUserId;
    private Long templateId;
    private String targetUserName;
    private String templateName;
    /** 岗位编码，如 OPERATOR */
    private String position;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private BigDecimal baseScore;
    private BigDecimal maxScore;
    private String grade;
    /** DRAFT / CONFIRMED */
    private String status;
    /** 前端审批流步骤 0~3（DRAFT=0, CONFIRMED=3） */
    private Integer workflowStatus;
    private String submittedAt;
    private String publishedAt;
    private String reviewer1;
    private String reviewer2;
    private List<PerfRecordItemDetailVO> items;
}
