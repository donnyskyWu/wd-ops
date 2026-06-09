package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PerfResultVO {

    private Long id;
    private Long userId;
    private String userName;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private String grade;
}
