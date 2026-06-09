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
    private Long templateId;
    private Long ipGroupId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private String grade;
    private String status;
    private LocalDateTime createTime;
}
