package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PerfRecordVO {

    private Long id;
    private Long targetUserId;
    private String targetUserName;
    /** 被考核人岗位 */
    private String position;
    private Long templateId;
    private Long ipGroupId;
    /** IP 组名称 */
    private String ipGroupName;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private String grade;
    private String status;
    /** 考核人名称 */
    private String evaluatorName;
    private LocalDateTime createTime;
}
