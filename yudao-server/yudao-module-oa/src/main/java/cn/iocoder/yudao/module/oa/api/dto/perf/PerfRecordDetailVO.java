package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PerfRecordDetailVO {

    private Long id;
    private String targetUserName;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalScore;
    private String grade;
    private String status;
    private List<PerfRecordItemDetailVO> items;
}
